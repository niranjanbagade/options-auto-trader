package com.trading.automated.nb.AutoTrader.telegram;

import com.trading.automated.nb.AutoTrader.entity.EntryEntity;
import com.trading.automated.nb.AutoTrader.enums.MessagePattern;
import com.trading.automated.nb.AutoTrader.services.ITradingAccount;
import com.trading.automated.nb.AutoTrader.services.PatternRecognitionService;
import com.trading.automated.nb.AutoTrader.services.SignalParserService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
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
public class TelegramBotStarter implements LongPollingBot {

    @Autowired
    private PatternRecognitionService patternRecognitionService;

    @Value("${telegram.bot.token}")
    private String botToken;

    private TelegramBot telegramBot;

    @Value("${telegram.channel.id}")
    private String targetChannelId;

    @Value("${broker.name}")
    private String brokerName;

    @Autowired
    private ITradingAccount tradingAccount;

    @Autowired
    private SignalParserService parser;

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotStarter.class);

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasChannelPost() && targetChannelId.equals(update.getChannelPost().getChatId().toString())) {
            try {
                String messageText = update.getChannelPost().getText();
                logger.info("Received Channel Post: " + messageText);
                MessagePattern messagePattern = patternRecognitionService.getMessagePattern(messageText);

                if(messagePattern.equals(MessagePattern.UNKNOWN_SIGNAL))
                    return;

                switch(messagePattern){
                    case ENTRY_SIGNAL:
                        logger.info("Entering trade");
                        EntryEntity entry = parser.getEntryEntity(messageText);
                        String tradeSymbol
                                = tradingAccount.getSymbol(entry.getStrikePrice(), entry.getExpiry());
                        logger.info("Result for symbol is :"+tradeSymbol);
                        String orderId = tradingAccount.placeOrder(tradeSymbol, entry.getAction(), entry.getOptionType());
                        logger.info("OrderId is : "+orderId);
                        break;
                    case EXIT_SQUARE_OFF_DUAL_LEG:
                        break;
                    case EXIT_SQUARE_OFF_SINGLE_LEG:
                        break;
                    case UNKNOWN_SIGNAL:
                        logger.warn("Unknown signal detected ",messageText.substring(0, Math.min(20, messageText.length())));
                        break;

                }
            } catch (Exception e) {
                logger.error("Error processing signal: " + e.getMessage());
            } catch (KiteException e) {
                logger.error("Exception from zerodha "+e.getMessage());
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