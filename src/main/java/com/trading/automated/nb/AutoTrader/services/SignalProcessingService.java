package com.trading.automated.nb.AutoTrader.services;

import com.trading.automated.nb.AutoTrader.entity.EntryEntity;
import com.trading.automated.nb.AutoTrader.entity.ExitEntity;
import com.trading.automated.nb.AutoTrader.enums.MessagePattern;
import com.trading.automated.nb.AutoTrader.exceptions.ApiException;
import com.trading.automated.nb.AutoTrader.services.master.MasterTrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// Assuming patternRecognitionService and SignalParserService are already autowired/available
@Service
public class SignalProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(SignalProcessingService.class);

    private final PatternRecognitionService patternRecognitionService;
    private final SignalParserService parser;
    private final MasterTrader masterTrader;

    public SignalProcessingService(PatternRecognitionService patternRecognitionService,
                                   SignalParserService parser,
                                   MasterTrader masterTrader) {
        this.patternRecognitionService = patternRecognitionService;
        this.parser = parser;
        this.masterTrader = masterTrader;
    }

    public void processSignal(String messageText) throws ApiException {
        MessagePattern messagePattern = patternRecognitionService.getMessagePattern(messageText);

        switch (messagePattern) {
            case ENTRY_SIGNAL:
                logger.info("Processing ENTRY_SIGNAL: {}", messageText);
                EntryEntity[] entryEntities = parser.getEntryParams(messageText);
                masterTrader.executeEntryTrade(entryEntities);
                break;

            case SQUARE_OFF_SIGNAL:
                logger.info("Processing SQUARE_OFF_SIGNAL: {}", messageText);
                ExitEntity[] exitEntities = parser.getExitParams(messageText);
                masterTrader.executeExitTrade(exitEntities);
                break;

            case UNKNOWN_SIGNAL:
                logger.warn("Skipping UNKNOWN_SIGNAL: {}", 
                            messageText.substring(0, Math.min(50, messageText.length())));
                break;
                
            default:
                logger.warn("Unhandled message pattern {}: Message preview: {}", 
                            messagePattern, messageText.substring(0, Math.min(50, messageText.length())));
                break;
        }
    }
}