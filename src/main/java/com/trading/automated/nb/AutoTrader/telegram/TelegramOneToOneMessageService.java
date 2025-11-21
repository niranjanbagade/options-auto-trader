package com.trading.automated.nb.AutoTrader.telegram;

import com.trading.automated.nb.AutoTrader.enums.MessageImportance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class TelegramOneToOneMessageService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramOneToOneMessageService.class);
    @Value("${telegram.bot.token}")
    private String botToken;
    public void sendMessage(String chatId,String message, MessageImportance importance) {
        try {
            String urlString = "https://api.telegram.org/bot" + botToken + "/sendMessage";
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

            // Simple error handling
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed to send Telegram message. HTTP error code: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error sending Telegram message: " + e.getMessage(), e);
        }
    }

    private String getSymbol(MessageImportance importance) {
        switch (importance) {
            case HIGH:
                return "❗";
            case MEDIUM:
                return "⚠️";
            case LOW:
                return "ℹ️";
            case GOOD:
                return "✅";
            default:
                return "";
        }
    }
}