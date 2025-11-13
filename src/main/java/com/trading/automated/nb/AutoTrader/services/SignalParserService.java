package com.trading.automated.nb.AutoTrader.services;

import com.trading.automated.nb.AutoTrader.entity.EntryEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service // Marking as a Spring Service
public class SignalParserService {

    private static final Logger logger = LoggerFactory.getLogger(SignalParserService.class);

    public EntryEntity getEntryEntity(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = Patterns.ENTRY_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                String action = matcher.group(1).toUpperCase().trim();
                String expiry = matcher.group(2).trim();
                String strike = matcher.group(3).trim();
                String optionType = strike.substring(strike.length()-2);
                double priceA = Double.parseDouble(matcher.group(4));
                double priceB = Double.parseDouble(matcher.group(5));
                logger.info("Parsed Entry Signal: Action={}, Strike={}, Range={}-{}",
                        action, strike,
                        Math.min(priceA, priceB),
                        Math.max(priceA, priceB));
                return new EntryEntity(action, expiry, strike, optionType, priceA, priceB);
            } catch (NumberFormatException e) {
                logger.error("Error parsing price values in signal message: {}", message, e);
                return null;
            } catch (Exception e) {
                logger.error("Unexpected error during signal parsing: {}", message, e);
                return null;
            }
        }
        logger.debug("Message did not match the ENTRY_PATTERN.");
        return null;
    }
}