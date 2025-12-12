package com.trading.automated.nb.AutoTrader.scheduler;

import com.trading.automated.nb.AutoTrader.services.TradingSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DailyScheduler.class);

    @Autowired
    private TradingSessionService tradingSessionService;

    // Schedule start
    @Scheduled(cron = "${app.scheduling.start-cron}", zone = "Asia/Kolkata")
    public void scheduleStart() {
        logger.info("Executing scheduled start task...");
        try {
            tradingSessionService.startSession();
        } catch (Exception e) {
            logger.error("Error occurred during scheduled start task: ", e);
        }
    }

    // Schedule stop
    @Scheduled(cron = "${app.scheduling.stop-cron}", zone = "Asia/Kolkata")
    public void scheduleStop() {
        logger.info("Executing scheduled stop task...");
        try {
            tradingSessionService.stopSession();
        } catch (Exception e) {
            logger.error("Error occurred during scheduled stop task: ", e);
        }
    }
}
