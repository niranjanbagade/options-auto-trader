package com.trading.automated.nb.AutoTrader.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.springframework.stereotype.Service;

@Service
public class TelegramAckService extends DefaultAbsSender {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.ack.channel.id}")
    private String targetChannel;

    private static final Logger logger = LoggerFactory.getLogger(TelegramAckService.class);

    protected TelegramAckService() {
        super(new DefaultBotOptions());
        this.botToken = "YOUR_BOT_TOKEN";  // Inject from config/env
        this.targetChannel = "@your_target_channel";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    /**
     * Publishes a text message to the configured Telegram channel.
     */
    public void postMessage(String messageText) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(targetChannel);
            message.setText(messageText);
            execute(message);
            logger.info("Published message "+ messageText + " to " + targetChannel);
        } catch (TelegramApiException e) {
            logger.error("Error while publishing to " + targetChannel + ": " + e.getMessage());
        }
    }
}
