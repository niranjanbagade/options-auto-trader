package com.trading.automated.nb.AutoTrader;

import com.trading.automated.nb.AutoTrader.config.MotilalTradingConfig;
import com.trading.automated.nb.AutoTrader.telegram.TelegramObserverBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
@ComponentScan
@EnableScheduling
@EnableConfigurationProperties(MotilalTradingConfig.class)
@EnableRetry
@EnableAsync
public class AutoTraderApplication implements CommandLineRunner {
	private static final Logger logger = LoggerFactory.getLogger(TelegramObserverBot.class);

	@Autowired
	private TelegramObserverBot telegramObserverBot;

	public static void main(String[] args) {
		SpringApplication.run(AutoTraderApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception{
		logger.debug("Starting Telegram bot polling.");
		try {
			TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
			botsApi.registerBot(telegramObserverBot);
		} catch (TelegramApiException e) {
			logger.error("Failed to start Telegram Bot: " + e.getMessage());
		}

		logger.debug("Bot is running.");
	}
}
