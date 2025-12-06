package com.trading.automated.nb.AutoTrader.telegram;

import com.trading.automated.nb.AutoTrader.enums.MessageImportance;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class TelegramOneToOneMessageService {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(TelegramOneToOneMessageService.class);

    @Value("${telegram.publisher.bot}")
    private String botToken;

    @Async("telegramExecutor")
    @Retryable(value = { RuntimeException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendMessage(String chatId, String message, MessageImportance importance) {
        try {
            String urlString = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            if (!chatId.startsWith("100"))
                chatId = "-100" + chatId;

            if (!chatId.startsWith("-"))
                chatId = "-" + chatId;

            message = getSymbol(importance) + " " + message;
            String payload = String.format("{\"chat_id\":\"%s\", \"text\":\"%s\"}", chatId, message);

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed to send Telegram message. HTTP error code: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            logger.error("Error sending Telegram message for chat id " + chatId, e);
            throw new RuntimeException("Error sending Telegram message: " + e.getMessage(), e);
        }
    }

    @Recover
    public void recover(RuntimeException e, String chatId, String message, MessageImportance importance) {
        // final failure handling after retries exhausted
        logger.error("Failed to send Telegram message after retries. chatId: {}, importance: {}, message: {}",
                chatId, importance, message, e);
        throw new RuntimeException("Failed to send Telegram message after retries", e);
    }

    private String getSymbol(MessageImportance importance) {
        return switch (importance) {
            case HIGH -> "❗";
            case MEDIUM -> "⚠️";
            case LOW -> "ℹ️";
            case GOOD -> "✅";
            default -> "";
        };
    }
}
