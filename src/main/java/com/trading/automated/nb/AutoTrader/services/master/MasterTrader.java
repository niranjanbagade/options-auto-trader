package com.trading.automated.nb.AutoTrader.services.master;

import com.trading.automated.nb.AutoTrader.cache.GlobalContextStore;
import com.trading.automated.nb.AutoTrader.config.MotilalTradingConfig;
import com.trading.automated.nb.AutoTrader.config.ZerodhaTradingConfig;
import com.trading.automated.nb.AutoTrader.entity.EntryEntity;
import com.trading.automated.nb.AutoTrader.entity.ExitEntity;
import com.trading.automated.nb.AutoTrader.enums.MessageImportance;
import com.trading.automated.nb.AutoTrader.exceptions.ApiException;
import com.trading.automated.nb.AutoTrader.services.ZerodhaTradeService;
import com.trading.automated.nb.AutoTrader.services.master.follower.ZerodhaFollowerService;
import com.trading.automated.nb.AutoTrader.telegram.TelegramOneToOneMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class MasterTrader {

    private static final Logger logger = LoggerFactory.getLogger(MasterTrader.class);

    @Autowired
    private TelegramOneToOneMessageService telegramService;

    @Autowired
    private MotilalTradingConfig motilalTradingConfig;

    @Autowired
    private ZerodhaTradingConfig zerodhaTradingConfig;

    @Autowired
    private ZerodhaTradeService zerodhaTradeService;

    @Autowired
    private GlobalContextStore globalContextStore;

    @Autowired
    private ZerodhaFollowerService zerodhaFollowerService;

    public void executeEntryTrade(EntryEntity[] entryEntiries) throws ApiException {
        executeEntryZerodhaClients(entryEntiries);
        executeEntryMotilalOswalClients(entryEntiries);
    }

    private void executeEntryMotilalOswalClients(EntryEntity[] entryEntiries) {
    }

    private void executeEntryZerodhaClients(EntryEntity[] entryEntities) {
        List<ZerodhaTradingConfig.ZerodhaAccount> accounts = zerodhaTradingConfig.getAccounts();
        long totalStartTime = System.nanoTime();
        for (EntryEntity entry : entryEntities){
            String tradeSymbol = zerodhaTradeService.getSymbol("Nifty " + entry.getStrike() + " " + entry.getOptionType(), entry.getExpiry());

            if(!globalContextStore.containsKey("zerodha_symbol"))
                globalContextStore.setValue("zerodha_symbol", tradeSymbol);

            for (ZerodhaTradingConfig.ZerodhaAccount account : accounts) {

                if(account.getAccessToken() == null || account.getAccessToken().isEmpty()){
                    logger.warn("Skipping order placement for account: {} as accessToken is missing.", account.getClientName());
                    telegramService.sendMessage(
                            account.getClientTelegramId(),
                            String.format("Skipping order placement for Zerodha account: %s as concent is not given.", account.getClientName()),
                            MessageImportance.LOW
                    );
                    continue;
                }

                if(account.getOnlyBuy() && entry.getAction().equalsIgnoreCase("SELL")){
                    logger.info("Skipping SELL order for account: {} as it is configured for ONLY BUY orders.", account.getClientName());
                    telegramService.sendMessage(
                            account.getClientTelegramId(),
                            String.format("Skipping SELL order for Zerodha account: %s as it is configured for ONLY BUY orders.", account.getClientName()),
                            MessageImportance.MEDIUM
                    );
                    continue;
                }

                long accountStartTime = System.nanoTime();
                CompletableFuture<Boolean> response = zerodhaFollowerService.placeOrderAsync(tradeSymbol, entry.getOptionType(), entry.getAction(), false, account, account.getAccessToken());
                long accountEndTime = System.nanoTime();
                long accountTimeMillis = (accountEndTime - accountStartTime) / 1_000_000;
                try {
                    if(response.get()){
                        logger.info("Order placed successfully for account: {} for symbol: {}", account.getClientName(), tradeSymbol);
                        String message = String.format("Entry trade processed for Zerodha account: %s in %d ms", account.getClientName(), accountTimeMillis);
                        telegramService.sendMessage(account.getClientTelegramId(), message, MessageImportance.GOOD);
                    } else {
                        logger.error("Order placement failed for account: {} for symbol: {}", account.getClientName(), tradeSymbol);
                        String message = String.format("Order placement failed for Zerodha account: %s for symbol: %s", account.getClientName(), tradeSymbol);
                        telegramService.sendMessage(account.getClientTelegramId(), message, MessageImportance.HIGH);
                    }
                } catch (Exception e) {
                    logger.error(String.format("Failed to send Telegram message to {}: {}", account.getClientTelegramId(), e.getMessage()));
                }
            }
        }
        long totalEndTime = System.nanoTime();
        long totalTimeMillis = (totalEndTime - totalStartTime) / 1_000_000;
        logger.info("Total time taken to process entry trades for all Zerodha accounts: {} ms", totalTimeMillis);
    }

    public void executeExitTrade(ExitEntity[] exitEntities) throws ApiException{
        executeExitZerodhaClients(exitEntities);
        executeExitMotilalOswalClients(exitEntities);
    }

    private void executeExitZerodhaClients(ExitEntity[] exitEntities) {
        for(ExitEntity exit : exitEntities){
            for(ZerodhaTradingConfig.ZerodhaAccount account : zerodhaTradingConfig.getAccounts()){
                String tradeSymbol = globalContextStore.getValue("zerodha_symbol");
                CompletableFuture<Boolean> response = zerodhaFollowerService.placeOrderAsync(tradeSymbol, exit.getOptionType(), exit.getAction(), true, account, account.getAccessToken());
                try {
                    if(response.get()){
                        logger.info("Exit Order placed successfully for account: {} for symbol: {}", account.getClientName(), tradeSymbol);
                        String message = String.format("Exit trade processed for Zerodha account: %s", account.getClientName());
                        telegramService.sendMessage(account.getClientTelegramId(), message, MessageImportance.GOOD);
                    } else {
                        logger.error("Exit Order placement failed for account: {} for symbol: {}", account.getClientName(), tradeSymbol);
                        String message = String.format("Exit Order placement failed for Zerodha account: %s for symbol: %s", account.getClientName(), tradeSymbol);
                        telegramService.sendMessage(account.getClientTelegramId(), message, MessageImportance.HIGH);
                    }
                } catch (Exception e) {
                    logger.error(String.format("Failed to send Telegram message to {}: {}", account.getClientTelegramId(), e.getMessage()));
                }
            }
        }

    }

    private void executeExitMotilalOswalClients(ExitEntity[] exitEntities) {

    }
}
