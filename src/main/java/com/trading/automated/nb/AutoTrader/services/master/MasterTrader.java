package com.trading.automated.nb.AutoTrader.services.master;

import com.trading.automated.nb.AutoTrader.cache.GlobalContextStore;
import com.trading.automated.nb.AutoTrader.dtos.UnifiedClientData;
import com.trading.automated.nb.AutoTrader.entity.EntryEntity;
import com.trading.automated.nb.AutoTrader.entity.ExitEntity;
import com.trading.automated.nb.AutoTrader.enums.MessageImportance;
import com.trading.automated.nb.AutoTrader.exceptions.ApiException;
import com.trading.automated.nb.AutoTrader.services.brokers.GrowwTradeService;
import com.trading.automated.nb.AutoTrader.services.brokers.ZerodhaTradeService;
import com.trading.automated.nb.AutoTrader.services.google.UnifiedClientDataService;
import com.trading.automated.nb.AutoTrader.services.master.follower.GrowwFollowerService;
import com.trading.automated.nb.AutoTrader.services.master.follower.ZerodhaFollowerService;
import com.trading.automated.nb.AutoTrader.telegram.TelegramOneToOneMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DeferredImportSelector.Group.Entry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
public class MasterTrader {

    private static final Logger logger = LoggerFactory.getLogger(MasterTrader.class);
    private static final String NSE_SYMBOL_KEY_LEG1 = "NSE_symbol_leg1";
    private static final String NSE_SYMBOL_KEY_LEG2 = "NSE_symbol_leg2";

    @Value("${master.account}")
    private String masterAccountEmail;

    @Autowired
    private TelegramOneToOneMessageService telegramService;

    @Autowired
    private ZerodhaTradeService zerodhaTradeService;

    @Autowired
    private GlobalContextStore globalContextStore;

    @Autowired
    private ZerodhaFollowerService zerodhaFollowerService;

    @Autowired
    private UnifiedClientDataService unifiedClientService;

    @Autowired
    private GrowwFollowerService growwFollowerService;

    public void executeEntryTrade(EntryEntity[] entryEntiries) throws ApiException {
        Map<String, UnifiedClientData> accounts = unifiedClientService.getMergedClientData();
        long totalStartTime = System.nanoTime();
        for (int i = 0; i < 2; i++) {
            EntryEntity entry = entryEntiries[i];
            String zerodhaSymbol = zerodhaTradeService
                    .getSymbol("Nifty " + entry.getStrike() + " " + entry.getOptionType(), entry.getExpiry());
            globalContextStore.setValue("zerodha_symbol_" + entry.getOptionType(), zerodhaSymbol);
            if (i == 0 && (globalContextStore.getValue(NSE_SYMBOL_KEY_LEG1) == null
                    || globalContextStore.getValue(NSE_SYMBOL_KEY_LEG1).isEmpty())) {
                globalContextStore.setValue(NSE_SYMBOL_KEY_LEG1, zerodhaSymbol);
            } else if (i == 1 && (globalContextStore.getValue(NSE_SYMBOL_KEY_LEG2) == null
                    || globalContextStore.getValue(NSE_SYMBOL_KEY_LEG2).isEmpty())) {
                globalContextStore.setValue(NSE_SYMBOL_KEY_LEG2, zerodhaSymbol);
            }

            for (Map.Entry<String, UnifiedClientData> accountEntry : accounts.entrySet()) {
                UnifiedClientData account = accountEntry.getValue();

                if ("OnlyOptionsBuying".equalsIgnoreCase(account.getClientPreference())
                        && entry.getAction().equalsIgnoreCase("Sell")) {
                    logger.info("Skipping Sell order for account: {} due to OnlyOptionsBuying preference",
                            account.getClientName());
                    continue;
                }

                long accountStartTime = System.nanoTime();
                CompletableFuture<Boolean> response = null;
                switch (account.getBroker().toLowerCase()) {
                    case "zerodha":
                        response = zerodhaFollowerService.placeOrderAsync(zerodhaSymbol, entry.getOptionType(),
                                entry.getAction(), false, account, account.getAccessToken());
                        break;
                    case "groww":
                        response = growwFollowerService.placeOrderAsync(zerodhaSymbol, entry.getOptionType(),
                                entry.getAction(), false, account, account.getAccessToken());
                        break;
                    default:
                        logger.error("Unsupported broker: {} for account: {}", account.getBroker(),
                                account.getClientName());
                        break;
                }
                long accountEndTime = System.nanoTime();
                long accountTimeMillis = (accountEndTime - accountStartTime) / 1_000_000;
                try {
                    if (response.get()) {
                        logger.info("Order placed successfully for account: {} for symbol: {}", account.getClientName(),
                                zerodhaSymbol);
                        String message = String.format("Entry trade processed for account: %s in %d ms",
                                account.getClientName(), accountTimeMillis);
                        telegramService.sendMessage(account.getTelegramChannelId(), message, MessageImportance.GOOD);
                    } else {
                        logger.error("Order placement failed for account: {} for symbol: {}", account.getClientName(),
                                zerodhaSymbol);
                        String message = String.format("Order placement failed for account: %s for symbol: %s",
                                account.getClientName(), zerodhaSymbol);
                        telegramService.sendMessage(account.getTelegramChannelId(), message, MessageImportance.HIGH);
                    }
                } catch (Exception e) {
                    logger.error("Failed to send Telegram message to {}: {}", account.getTelegramChannelId(),
                            e.getMessage());
                }
            }
        }

        long totalEndTime = System.nanoTime();
        long totalTimeMillis = (totalEndTime - totalStartTime) / 1_000_000;
        logger.info("Total time taken to process entry trades for all Zerodha accounts: {} ms", totalTimeMillis);
    }

    public void executeExitTrade(ExitEntity[] exitEntities) throws ApiException {
        for (ExitEntity exit : exitEntities) {
            Map<String, UnifiedClientData> accounts = unifiedClientService.getMergedClientData();
            for (Map.Entry<String, UnifiedClientData> accountEntry : accounts.entrySet()) {
                long accountStartTime = System.nanoTime();
                UnifiedClientData account = accountEntry.getValue();

                if ("OnlyOptionsBuying".equalsIgnoreCase(account.getClientPreference())
                        && exit.getAction().equalsIgnoreCase("Buy")) {
                    logger.debug("Skipping Sell order for account: {} due to OnlyOptionsBuying preference",
                            account.getClientName());
                    continue;
                }

                String tradeSymbol = globalContextStore.getValue("zerodha_symbol_" + exit.getOptionType());

                final String action = exit.getAction();
                final String clientName = account.getClientName();

                final String key = clientName + "_" + tradeSymbol;
                final String value = globalContextStore.getValue(key);
                if (value == null) {
                    String message = "No existing position found for square off: " + tradeSymbol;
                    logger.info("{} for {}", message, clientName);
                    telegramService.sendMessage(account.getTelegramChannelId(), message, MessageImportance.HIGH);
                    continue;
                }
                boolean validSquareOff = ("BUY".equals(value) && "SELL".equals(action)) ||
                        ("SELL".equals(value) && "BUY".equals(action));
                if (validSquareOff) {
                    logger.info("Square off {} position for: {} for {}", value, tradeSymbol, clientName);
                } else {
                    String message = "No matching position found to square off for: " + tradeSymbol;
                    logger.info("{} for {}", message, clientName);
                    telegramService.sendMessage(account.getTelegramChannelId(), message, MessageImportance.HIGH);
                    continue;
                }

                CompletableFuture<Boolean> response = null;
                switch (account.getBroker().toLowerCase()) {
                    case "zerodha":
                        response = zerodhaFollowerService.placeOrderAsync(tradeSymbol, exit.getOptionType(),
                                exit.getAction(), true, account, account.getAccessToken());
                        break;
                    case "groww":
                        response = growwFollowerService.placeOrderAsync(tradeSymbol, exit.getOptionType(),
                                exit.getAction(), true, account, account.getAccessToken());
                        break;
                    default:
                        logger.error("Unsupported broker: {} for account: {}", account.getBroker(),
                                account.getClientName());
                        continue;
                }
                long accountEndTime = System.nanoTime();
                long accountTimeMillis = (accountEndTime - accountStartTime) / 1_000_000;
                try {
                    if (response.get()) {
                        logger.info("Exit Order placed successfully for account: {} for symbol: {}",
                                account.getClientName(), tradeSymbol);
                        String message = String.format("Exit trade processed for account: %s in %d ms",
                                account.getClientName(), accountTimeMillis);
                        globalContextStore.removeKey(key);
                        telegramService.sendMessage(account.getTelegramChannelId(), message, MessageImportance.GOOD);
                    } else {
                        logger.error("Exit Order placement failed for account: {} for symbol: {}",
                                account.getClientName(), tradeSymbol);
                        String message = String.format("Exit Order placement failed for account: %s for symbol: %s",
                                account.getClientName(), tradeSymbol);
                        telegramService.sendMessage(account.getTelegramChannelId(), message, MessageImportance.HIGH);
                    }
                } catch (Exception e) {
                    logger.error("Failed to send Telegram message to {}: {}", account.getTelegramChannelId(),
                            e.getMessage());
                }
            }
        }
        globalContextStore.removeKey(NSE_SYMBOL_KEY_LEG1);
        globalContextStore.removeKey(NSE_SYMBOL_KEY_LEG2);
        globalContextStore.removeKey("zerodha_symbol");
        globalContextStore.removeKey("groww_symbol");
        // globalContextStore.removeKey("motilal_symbol");
    }

    @Async
    public void startTrailing(ExitEntity[] exitEntities) {
        logger.info("Starting trailing");
        boolean keepTrailing = false;
        Set<String> accountKeys = unifiedClientService.getMergedClientData().keySet();
        UnifiedClientData account = unifiedClientService.getMergedClientData().get(accountKeys.iterator().next());
        double previousLtp = 0;
        double lastFetchedLtp = 0;
        int sameLtpCount = 0;
        int sleepTime = 1000;
        int trailFactor = 2;
        if (account != null) {
            keepTrailing = true;
        }
        while (keepTrailing) {
            try {
                String symbol = globalContextStore.getValue(NSE_SYMBOL_KEY_LEG1);
                if (symbol == null || symbol.isEmpty()) {
                    logger.error("No symbol found for trailing");
                    break;
                }
                double currentLtp = growwFollowerService.getLtp(account, symbol);

                if (currentLtp == lastFetchedLtp) {
                    sameLtpCount++;
                    logger.info("LTP is same for {} consecutive times", sameLtpCount);
                    if (sameLtpCount >= 5) {
                        logger.info("Exiting all trades as LTP is same for 5 consecutive times at price {}",
                                currentLtp);
                        exitAllTrades(exitEntities);
                        keepTrailing = false;
                    }
                } else {
                    sameLtpCount = 0;
                    lastFetchedLtp = currentLtp;
                }

                logger.info("The difference between current {} and previous LTP {} is {}", currentLtp, previousLtp,
                        currentLtp - previousLtp);
                if (previousLtp == 0) {
                    previousLtp = currentLtp;
                } else if (currentLtp > previousLtp + trailFactor) {
                    previousLtp = currentLtp;
                    logger.info("Trailing SL moved up to price {}", currentLtp);
                } else if (currentLtp < previousLtp - trailFactor) {
                    logger.info("Exiting all trades as trailing SL hit at price {}", currentLtp);
                    exitAllTrades(exitEntities);
                    keepTrailing = false;
                }
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                logger.error("Trailing interrupted", e);
                keepTrailing = false;
            }
        }
        logger.info("Trailing stopped");
    }

    private void exitAllTrades(ExitEntity[] exitEntities) {
        logger.info("Exiting all trades");
        try {
            executeExitTrade(exitEntities);
        } catch (ApiException e) {
            logger.error("Failed to execute exit trade", e);
        }
    }
}
