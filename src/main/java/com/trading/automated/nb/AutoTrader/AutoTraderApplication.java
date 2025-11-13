package com.trading.automated.nb.AutoTrader;

import com.trading.automated.nb.AutoTrader.telegram.TelegramBotStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
@ComponentScan
public class AutoTraderApplication implements CommandLineRunner {
	private static final Logger logger = LoggerFactory.getLogger(TelegramBotStarter.class);

	@Autowired
	private TelegramBotStarter telegramBotStarter;

	public static void main(String[] args) {
		SpringApplication.run(AutoTraderApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception{
		logger.debug("Starting Telegram bot polling.");
		try {
			TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
			botsApi.registerBot(telegramBotStarter);
		} catch (TelegramApiException e) {
			logger.error("Failed to start Telegram Bot: " + e.getMessage());
		}

		logger.debug("Bot is running.");
	}
}
