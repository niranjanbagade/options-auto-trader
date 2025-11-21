package com.trading.automated.nb.AutoTrader.services;

import com.trading.automated.nb.AutoTrader.enums.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import static com.trading.automated.nb.AutoTrader.enums.Patterns.*;


@Service
public class PatternRecognitionService {

    private static final Logger logger = LoggerFactory.getLogger(PatternRecognitionService.class);

    public MessagePattern getMessagePattern(String cleanMessage) {
        if (cleanMessage.isEmpty()) {
            return MessagePattern.UNKNOWN_SIGNAL;
        }
        if (SQUARE_OFF_PATTERN.matcher(cleanMessage).find()) {
            return MessagePattern.SQUARE_OFF_SIGNAL;
        }
        if (ENTRY_PATTERN.matcher(cleanMessage).find()) {
            return MessagePattern.ENTRY_SIGNAL;
        }
        return MessagePattern.UNKNOWN_SIGNAL;
    }
}