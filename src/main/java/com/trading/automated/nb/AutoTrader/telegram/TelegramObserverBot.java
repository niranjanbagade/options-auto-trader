package com.trading.automated.nb.AutoTrader.telegram;

import com.trading.automated.nb.AutoTrader.cache.GlobalContextStore;
import com.trading.automated.nb.AutoTrader.entity.EntryEntity;
import com.trading.automated.nb.AutoTrader.entity.ExitEntity;
import com.trading.automated.nb.AutoTrader.enums.MessagePattern;
import com.trading.automated.nb.AutoTrader.exceptions.ApiException;
import com.trading.automated.nb.AutoTrader.services.PatternRecognitionService;
import com.trading.automated.nb.AutoTrader.services.SignalParserService;
import com.trading.automated.nb.AutoTrader.services.master.MasterTrader;
import jakarta.annotation.PostConstruct;
import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramObserverBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramObserverBot.class);

    @Autowired
    private PatternRecognitionService patternRecognitionService;
    @Autowired
    private SignalParserService parser;
    @Autowired
    private GlobalContextStore globalContextStore;
    @Autowired
    private MasterTrader masterTrader;

    @Value("${telegram.listener.bot}")
    private String botToken;

    @Value("${telegram.listener.bot.username}")
    private String listenerBotUsername;

    @Value("${telegram.channel.id}")
    private String targetChannelId;

    @Value("${spring.profiles.active:}") // Inject active profiles, or empty string if none
    private String activeProfiles;

    @PostConstruct
    public void sendStartupMessage() {
        SendMessage message = new SendMessage();
        message.setChatId(targetChannelId); // should be the channel ID, e.g., "-1001234567890"
        message.setText("bot is listening for signals...");
        try {
            if (!"prod".equalsIgnoreCase(activeProfiles)) {
                message.setText("bot is listening for signals... [Profiles: " + activeProfiles + "]");
                execute(message);
            }
        } catch (TelegramApiException e) {
            logger.error("Error sending startup message", e);
        }
    }


    public TelegramObserverBot(@Value("${telegram.listener.bot}") String botToken) {
        super(getBotOptions());
        this.botToken = botToken;
    }

    private static DefaultBotOptions getBotOptions() {
        DefaultBotOptions options = new DefaultBotOptions();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(30000)  // 30 seconds
                .setSocketTimeout(30000)   // 30 seconds
                .build();
        options.setRequestConfig(requestConfig);
        return options;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasChannelPost() && targetChannelId.equals(update.getChannelPost().getChatId().toString())) {
                int updateDate = update.getChannelPost().getDate(); // seconds since epoch (UTC)
                long now = System.currentTimeMillis() / 1000L; // current seconds since epoch (UTC)
                if (now - updateDate > 30) {
                    logger.info("Skipping message older than 30 seconds. Message date: {}, Now: {}, Difference: {} seconds",
                            updateDate, now, now - updateDate);
                    return;
                }

                String messageText = update.getChannelPost().getText();
                logger.info("Received Channel Post: {}", messageText);

                if (messageText == null) {
                    logger.info("Skipping Channel Post because it contains no text.");
                    return;
                }

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
                        break;
                    case UNKNOWN_SIGNAL:
                        logger.warn("Unknown signal detected: {}", messageText.substring(0, Math.min(20, messageText.length())));
                        break;
                    default:
                        logger.warn("Unhandled message pattern: {}", messagePattern);
                        break;
                }
            }
        } catch (ApiException e) {
            logger.error("API Error processing signal", e);
        } catch (Exception e) {
            logger.error("Error processing signal", e);
        }
    }

    @Override
    public String getBotUsername() {
        return listenerBotUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}