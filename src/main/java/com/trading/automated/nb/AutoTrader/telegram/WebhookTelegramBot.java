package com.trading.automated.nb.AutoTrader.telegram;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.trading.automated.nb.AutoTrader.exceptions.ApiException;
import com.trading.automated.nb.AutoTrader.services.SignalProcessingService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
public class WebhookTelegramBot extends TelegramWebhookBot {

    private static final Logger logger = LoggerFactory.getLogger(WebhookTelegramBot.class);
    // Inject values from application.properties
    @Value("${telegram.listener.bot.username}")
    private String botUsername;

    @Value("${telegram.listener.bot}")
    private String botToken;

    @Value("${telegram.bot.webhook-path}")
    private String botPath;

    @Autowired
    private SignalProcessingService signalProcessingService;

    private final ExecutorService signalExecutor = Executors.newFixedThreadPool(5); 

    /**
     * This method is the core REST endpoint handler.
     * When Telegram sends an update (a message), the Spring-managed controller
     * calls this method, passing the parsed Update object.
     * The method must return a BotApiMethod (e.g., a SendMessage object)
     * as Telegram expects a response immediately for the webhook to succeed.
     */
    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        // 1. Determine the relevant message object (either message or channelPost)
        Message message = null;

        if (update.hasMessage()) {
            message = update.getMessage();
        } else if (update.hasChannelPost()) {
            message = update.getChannelPost();
        }

        // 2. Process the extracted message if it exists and has text
        if (message != null && message.hasText() && message.getDate() > (System.currentTimeMillis() / 1000) - 15) {
            String messageText = message.getText();

            // IMPORTANT: For a channel post, you must use the channel ID for the response
            long chatId = message.getChatId();

            // Simple logic: echo the message back
            String responseText = "Received post from channel: " + messageText;

            logger.info("Received message from chatId: {} and the message is {}", chatId, messageText);

            // Create the response object
            SendMessage responseMessage = new SendMessage();
            responseMessage.setChatId(String.valueOf(chatId));
            responseMessage.setText(responseText);

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

            // Return null as you handled the response via execute()
            return null;
        } else {
            logger.info("Received non-text or an old update: {}", message != null ? message.hasText() + " " + message.getText() : "null");
        }

        // Return null if no response is needed (e.g., for non-text updates)
        return null;
    }

    // --- Configuration Methods Required by the Library ---

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    /**
     * The path under which this bot's webhook will be accessed.
     * e.g., https://your-public-url/update/YOUR_SECRET_PATH
     */
    @Override
    public String getBotPath() {
        return botPath;
    }
}