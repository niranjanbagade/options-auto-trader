package com.trading.automated.nb.AutoTrader.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Service
public class TelegramConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramConnectionService.class);

    private final TelegramObserverBot telegramObserverBot;

    // Initial wait time: 0.5 seconds (500 milliseconds)
    private static final long INITIAL_WAIT_TIME_MS = 500;
    // Maximum wait time: 6 seconds (6000 milliseconds)
    private static final long MAX_WAIT_TIME_MS = 6000;

    public TelegramConnectionService(TelegramObserverBot telegramObserverBot) {
        this.telegramObserverBot = telegramObserverBot;
    }

    private volatile boolean isConnectionStarted = false;

    @Async
    public void connect() {
        if (isConnectionStarted) {
            logger.warn("Telegram bot is already connected. Skipping reconnection.");
            return;
        }
        isConnectionStarted = true;
        logger.info("Starting Telegram bot polling with automatic reconnection logic.");

        long currentWaitTime = INITIAL_WAIT_TIME_MS;

        // Loop forever, attempting to register and run the bot
        while (true) {
            try {
                logger.info("Attempting to register Telegram bot...");

                // 1. Create a new API session on each attempt
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

                // 2. Register the bot. This call blocks if successful.
                botsApi.registerBot(telegramObserverBot);

                logger.info("Telegram Bot registered successfully and is now running.");

                // If registration succeeds, the bot runs in the background thread
                // and the main thread stays blocked here. We break the loop
                // because we assume the bot will run indefinitely or throw
                // an exception that we can catch.
                break;

            } catch (TelegramApiException e) {
                // 3. Catch the exception (like NoRouteToHost, ConnectException, etc.)
                logger.error("FATAL: Telegram API connection failed. Attempting reconnection.", e);

                // 4. Wait using exponential backoff
                logger.info("Attempting to reconnect in {} seconds...", currentWaitTime / 1000);

                try {
                    Thread.sleep(currentWaitTime);
                } catch (InterruptedException ie) {
                    logger.warn("Reconnection thread interrupted.", ie);
                    Thread.currentThread().interrupt();
                    return; // Exit the method gracefully if interrupted
                }

                // 5. Increase the wait time for the next attempt (Exponential Backoff)
                currentWaitTime = Math.min(currentWaitTime * 2, MAX_WAIT_TIME_MS);
            }
        }

        logger.debug("Telegram connection service finished. Bot background thread is active.");
    }
}
