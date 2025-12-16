package com.trading.automated.nb.AutoTrader.controller;

import com.trading.automated.nb.AutoTrader.telegram.WebhookTelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class TelegramController {

    private final WebhookTelegramBot telegramBot;

    @Autowired
    public TelegramController(WebhookTelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @PostMapping("${telegram.bot.webhook-path}")
    public BotApiMethod<?> onUpdateReceived(@RequestBody Update update) {
        return telegramBot.onWebhookUpdateReceived(update);
        // return null;
    }
}
