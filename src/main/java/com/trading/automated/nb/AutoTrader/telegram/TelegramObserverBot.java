package com.trading.automated.nb.AutoTrader.telegram;

import com.trading.automated.nb.AutoTrader.cache.GlobalContextStore;
import com.trading.automated.nb.AutoTrader.entity.EntryEntity;
import com.trading.automated.nb.AutoTrader.entity.ExitEntity;
import com.trading.automated.nb.AutoTrader.enums.MessagePattern;
import com.trading.automated.nb.AutoTrader.exceptions.ApiException;
import com.trading.automated.nb.AutoTrader.services.PatternRecognitionService;
import com.trading.automated.nb.AutoTrader.services.SignalParserService;
import com.trading.automated.nb.AutoTrader.services.master.MasterTrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.BotOptions;
import org.telegram.telegrambots.meta.generics.LongPollingBot;
import org.telegram.telegrambots.meta.generics.TelegramBot;

@Component
public class TelegramObserverBot implements LongPollingBot {
    @Autowired
    private PatternRecognitionService patternRecognitionService;
    @Value("${telegram.bot.token}")
    private String botToken;
    private TelegramBot telegramBot;
    @Value("${telegram.channel.id}")
    private String targetChannelId;
    @Autowired
    private SignalParserService parser;
    @Autowired
    GlobalContextStore globalContextStore;
    @Autowired
    private MasterTrader masterTrader;

    private static final Logger logger = LoggerFactory.getLogger(TelegramObserverBot.class);

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasChannelPost() && targetChannelId.equals(update.getChannelPost().getChatId().toString())) {
            try {
                String messageText = update.getChannelPost().getText();
                logger.info("Received Channel Post: " + messageText);
                MessagePattern messagePattern = patternRecognitionService.getMessagePattern(messageText);

                if (messagePattern.equals(MessagePattern.UNKNOWN_SIGNAL))
                    return;

                switch (messagePattern) {
                    case ENTRY_SIGNAL:
                        logger.info("Entering trade");
                        EntryEntity[] entities = parser.getEntryParams(messageText);
                        masterTrader.executeEntryTrade(entities);
                        break;
                    case SQUARE_OFF_SIGNAL:
                        ExitEntity[] exitEntities = parser.getExitParams(messageText);
                        masterTrader.executeExitTrade(exitEntities);
//                        for (ExitEntity exitEntity : exitEntities) {
//                            String dualLegExitSymbol = tradingAccount.getSymbol("Nifty " + exitEntity.getStrike() + " " + exitEntity.getOptionType(), globalContextStore.getValue("expiry"));
//                            if(exitEntity.getAction().equalsIgnoreCase("Sell") && onlyOptionsBuying){
//                                String dualLegExitOrderId = tradingAccount.placeOrder(dualLegExitSymbol, exitEntity.getAction(), exitEntity.getOptionType(), true);
//                                logger.info("Dual Leg Exit OrderId is : " + dualLegExitOrderId);
//                            }else{
//                                logger.info("Exit Action is {}", exitEntity.getAction(), "only options buying flag {}",onlyOptionsBuying);
//                                String dualLegExitOrderId = tradingAccount.placeOrder(dualLegExitSymbol, exitEntity.getAction(), exitEntity.getOptionType(), true);
//                                logger.info("Dual Leg Exit OrderId is : " + dualLegExitOrderId);
//                            }
//                        }
                        break;
                    case UNKNOWN_SIGNAL:
                        logger.warn("Unknown signal detected ", messageText.substring(0, Math.min(20, messageText.length())));
                        break;
                }
//                telegramAckService.postMessage("Processed signal: " + messageText.substring(0, Math.min(50, messageText.length())));
            } catch (ApiException e){
//                telegramAckService.postMessage("API Error processing signal: " + e.toString());
                logger.error("API Error processing signal: " + e.toString());
            } catch (Exception e) {
//                telegramAckService.postMessage("Error processing signal: " + e.getMessage());
                logger.error("Error processing signal: " + e.getMessage());
            }
        }
    }

    @Override
    public BotOptions getOptions() {
        return new DefaultBotOptions();
    }

    @Override
    public void clearWebhook() throws TelegramApiRequestException {

    }

    @Override
    public String getBotUsername() {
        return "TradingBot909";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}