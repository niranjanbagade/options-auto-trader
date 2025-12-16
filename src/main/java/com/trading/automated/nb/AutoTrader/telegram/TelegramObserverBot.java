package com.trading.automated.nb.AutoTrader.telegram;

import com.trading.automated.nb.AutoTrader.exceptions.ApiException;
import com.trading.automated.nb.AutoTrader.services.SignalProcessingService; // New Service
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TelegramObserverBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramObserverBot.class);
    
    // Executor for asynchronous signal processing (to avoid blocking Telegram's thread)
    // Using fixed thread pool, size appropriate for expected load.
    private final ExecutorService signalExecutor = Executors.newFixedThreadPool(5); 

    private final SignalProcessingService signalProcessingService;
    private final String botToken;
    private final String listenerBotUsername;
    private final String targetChannelId;

    // Use constructor injection for all dependencies and @Value properties
    public TelegramObserverBot(
            @Lazy DefaultBotOptions botOptions, // Inject DefaultBotOptions as a bean
            SignalProcessingService signalProcessingService, // New service to handle logic
            @Value("${telegram.listener.bot}") String botToken,
            @Value("${telegram.listener.bot.username}") String listenerBotUsername,
            @Value("${telegram.channel.id}") String targetChannelId) { // Default to 'dev'
        
        super(botOptions); // Pass the injected options to the superclass
        this.signalProcessingService = signalProcessingService;
        this.botToken = botToken;
        this.listenerBotUsername = listenerBotUsername;
        this.targetChannelId = targetChannelId;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Step 1: Filter update type and channel ID
        if (update.hasChannelPost() && targetChannelId.equals(update.getChannelPost().getChatId().toString())) {
            
            String messageText = update.getChannelPost().getText();
            
            if (messageText == null) {
                logger.debug("Skipping Channel Post because it contains no text.");
                return;
            }

            int updateDate = update.getChannelPost().getDate();
            long now = System.currentTimeMillis() / 1000L;
            
            // Step 2: Time check
            if (now - updateDate > 30) {
                logger.info("Skipping outdated message ({} seconds old).", now - updateDate);
                return;
            }

            logger.info("Received signal for processing: {}", messageText);

            // Step 3: Asynchronously process signal
            signalExecutor.submit(() -> {
                try {
                    signalProcessingService.processSignal(messageText);
                } catch (ApiException e) {
                    logger.error("API Error during signal processing for message: {}", messageText, e);
                } catch (Exception e) {
                    logger.error("Unexpected error during signal processing for message: {}", messageText, e);
                }
            });

        } else if (update.hasMessage() && update.getMessage().isUserMessage()) {
             // Optional: Handle direct messages to the bot (e.g., status request, manual command)
             logger.debug("Received direct message from user: {}", update.getMessage().getFrom().getUserName());
             // Implement logic for responding to direct user commands if needed
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