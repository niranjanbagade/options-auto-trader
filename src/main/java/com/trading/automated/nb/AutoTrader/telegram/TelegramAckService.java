package com.trading.automated.nb.AutoTrader.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class TelegramAckService {
    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.ack.channel.id}")
    private String ackChatId;

    private static final Logger logger = LoggerFactory.getLogger(TelegramAckService.class);

    private static volatile TelegramAckService instance;

    private TelegramAckService() {
        // Initialize connection with Telegram channel using botToken
        initializeConnection();
    }

    public static TelegramAckService getInstance() {
        if (instance == null) {
            synchronized (TelegramAckService.class) {
                if (instance == null) {
                    instance = new TelegramAckService();
                }
            }
        }
        return instance;
    }

    private void initializeConnection() {
        int maxRetries = 3;
        int attempt = 0;
        boolean success = false;

        while (attempt < maxRetries && !success) {
            try {
                attempt++;
                // Simulate connection initialization (e.g., validating bot token)
                logger.info("Attempting to initialize connection with Telegram (Attempt " + attempt + ")");
                String testUrl = "https://api.telegram.org/bot" + botToken + "/getMe";
                HttpURLConnection connection = (HttpURLConnection) new URL(testUrl).openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    logger.info("Successfully initialized connection with Telegram Bot API.");
                    success = true;
                } else {
                    logger.error("Failed to initialize connection. Response code: " + responseCode);
                }
            } catch (Exception e) {
                logger.error("Error during Telegram connection initialization: " + e.getMessage(), e);
            }

            if (!success && attempt < maxRetries) {
                try {
                    Thread.sleep(2000); // Delay before retry
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (!success) {
            logger.error("Failed to initialize connection after " + maxRetries + " attempts.");
        }
    }

    public void postMessage(String message) {
        int maxRetries = 3;
        int attempt = 0;
        boolean success = false;

        while (attempt < maxRetries && !success) {
            try {
                attempt++;
                logger.info("Attempting to post message to Telegram (Attempt " + attempt + ")");
                String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
                String payload = String.format("{\"chat_id\": \"%s\", \"text\": \"%s\"}", ackChatId, message);

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.getBytes());
                    os.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    logger.info("Message successfully posted to Telegram channel.");
                    success = true;
                } else {
                    logger.error("Failed to post message. Response code: " + responseCode);
                }
            } catch (Exception e) {
                logger.error("Error while posting message to Telegram: " + e.getMessage(), e);
            }

            if (!success && attempt < maxRetries) {
                try {
                    Thread.sleep(2000); // Delay before retry
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (!success) {
            logger.error("Failed to post message after " + maxRetries + " attempts.");
        }
    }
}

