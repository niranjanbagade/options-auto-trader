package com.trading.automated.nb.AutoTrader.services;

import com.trading.automated.nb.AutoTrader.services.google.ActiveClientsService;
import com.trading.automated.nb.AutoTrader.services.google.DailyConsentService;
import com.trading.automated.nb.AutoTrader.services.google.UnifiedClientDataService;
import com.trading.automated.nb.AutoTrader.telegram.TelegramConnectionService;
import com.trading.automated.nb.AutoTrader.telegram.TelegramOneToOneMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TradingSessionService {

    private static final Logger logger = LoggerFactory.getLogger(TradingSessionService.class);

    @Autowired
    private ActiveClientsService activeClientsService;

    @Autowired
    private DailyConsentService dailyConsentService;

    @Autowired
    private UnifiedClientDataService unifiedClientDataService;

    @Autowired
    private TelegramConnectionService telegramConnectionService;

    @Autowired
    private TelegramOneToOneMessageService telegramOneToOneMessageService;

    @Value("${telegram.bot.connection.type}")
    private String connectionType;

    public void startSession() throws Exception {
        logger.info("Starting trading session...");
        activeClientsService.init();
        dailyConsentService.init();
        unifiedClientDataService.init();
        if (!connectionType.equalsIgnoreCase("webhook")) {
            telegramConnectionService.connect();
        } else {
            logger.info("Webhook connection type is configured.");
        }
        logger.info("Trading session started successfully.");
    }

    public void stopSession() throws Exception {
        logger.info("Stopping trading session...");
        unifiedClientDataService.getMergedClientData().values().forEach(client -> {
            telegramOneToOneMessageService.sendMessage(
                    client.getTelegramChannelId(),
                    "Trading session has ended. See you tomorrow!",
                    com.trading.automated.nb.AutoTrader.enums.MessageImportance.LOW);
        });
        logger.info("Trading session stopped and goodbye messages sent.");
    }

    public void refreshSession() throws Exception {
        logger.info("Refreshing trading session...");
        activeClientsService.init();
        dailyConsentService.init();
        unifiedClientDataService.init();
        logger.info("Trading session refreshed successfully.");
    }
}
