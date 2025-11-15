package com.trading.automated.nb.AutoTrader.services;

import com.trading.automated.nb.AutoTrader.cache.GlobalContextStore;
import com.trading.automated.nb.AutoTrader.entity.EntryEntity;
import com.trading.automated.nb.AutoTrader.entity.ExitEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service
public class SignalParserService {

    private static final Logger logger = LoggerFactory.getLogger(SignalParserService.class);

    @Autowired
    private GlobalContextStore globalContextStore;

    // Method to replace all new line characters with spaces and reduce multiple spaces to a single space
    private String normalizeWhitespace(String input) {
        if (input == null) {
            return null;
        }
        // Replace new line characters with spaces and reduce multiple spaces to a single space
        return input.replaceAll("\\s+", " ").trim();
    }

    public EntryEntity[] getEntryParams(String message) {
        if (message == null) {
            return null;
        }

        message = normalizeWhitespace(message);

        // Try matching dual trade pattern first
        EntryEntity[] dualTrade = matchDualTradePattern(message);
        if (dualTrade != null) {
            return dualTrade;
        }

        // Try matching single trade pattern
        EntryEntity[] singleTrade = matchSingleTradePattern(message);
        if (singleTrade != null) {
            return singleTrade;
        }

        logger.debug("Message did not match any known ENTRY_PATTERN.");
        return null;
    }

    private EntryEntity[] matchSingleTradePattern(String message) {
        String pattern = "FRESH TRADE\\s+\"(BUY|SELL)\"\\s(\\d+\\s\\w+)\\s\"Nifty\\s(\\d+)\\s(CE|PE)\"\\sbetween\\s(\\d+\\.?\\d*)\\s-\\s(\\d+\\.?\\d*)\\s.*?Stop\\sloss\\sfor\\s\\d+\\s(CE|PE)\\sis\\s(\\d+\\.?\\d*)";
        Matcher matcher = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL).matcher(message);

        if (matcher.find()) {
            try {
                String action = matcher.group(1).toUpperCase().trim();
                String expiry = matcher.group(2).trim();
                String strike = matcher.group(3).trim();
                String optionType = matcher.group(4).toUpperCase().trim();
                double priceA = Double.parseDouble(matcher.group(5));
                double priceB = Double.parseDouble(matcher.group(6));
                double stopLoss = Double.parseDouble(matcher.group(8));

                EntryEntity entry = new EntryEntity(action, expiry, strike, optionType, priceA, priceB, stopLoss);
                globalContextStore.setValue("expiry", expiry);
                return new EntryEntity[]{entry};
            } catch (NumberFormatException e) {
                logger.error("Error parsing price or stop loss values in single trade pattern: {}", message, e);
            } catch (Exception e) {
                logger.error("Unexpected error during single trade parsing: {}", message, e);
            }
        }

        return null;
    }

    private EntryEntity[] matchDualTradePattern(String message) {
        String pattern = "FRESH TRADE\\s+\"(BUY|SELL)\"\\s(\\d+\\s\\w+)\\s\"Nifty\\s(\\d+)\\s(CE|PE)\"\\sbetween\\s(\\d+\\.?\\d*)\\s-\\s(\\d+\\.?\\d*)\\sAND\\s\"(BUY|SELL)\"\\s(\\d+\\s\\w+)\\s\"Nifty\\s(\\d+)\\s(CE|PE)\"\\sbetween\\s(\\d+\\.?\\d*)\\s-\\s(\\d+\\.?\\d*)\\sStop\\sloss\\sfor\\s(\\d+)\\s(CE|PE)\\sis\\s(\\d+\\.?\\d*)\\sand\\s(\\d+)\\s(CE|PE)\\sis\\s(\\d+\\.?\\d*)";
        Matcher matcher = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL).matcher(message);
        logger.info("Regex Pattern: {}", pattern);
        if (matcher.find()) {
            try {

                // First trade details
                String action1 = matcher.group(1).toUpperCase().trim();
                String expiry1 = matcher.group(2).trim();
                String strike1 = matcher.group(3).trim();
                String optionType1 = matcher.group(4).toUpperCase().trim();
                double priceA1 = Double.parseDouble(matcher.group(5));
                double priceB1 = Double.parseDouble(matcher.group(6));
                double stopLoss1 = Double.parseDouble(matcher.group(15));
                globalContextStore.setValue("expiry", expiry1);
                EntryEntity entry1 = new EntryEntity(action1, expiry1, strike1, optionType1, priceA1, priceB1, stopLoss1);

                // Second trade details
                String action2 = matcher.group(7).toUpperCase().trim();
                String expiry2 = matcher.group(8).trim();
                String strike2 = matcher.group(9).trim();
                String optionType2 = matcher.group(10).toUpperCase().trim();
                double priceA2 = Double.parseDouble(matcher.group(11));
                double priceB2 = Double.parseDouble(matcher.group(12));
                double stopLoss2 = Double.parseDouble(matcher.group(18));
                globalContextStore.setValue("expiry", expiry2);
                EntryEntity entry2 = new EntryEntity(action2, expiry2, strike2, optionType2, priceA2, priceB2, stopLoss2);

                return new EntryEntity[]{entry1, entry2};
            } catch (NumberFormatException e) {
                logger.error("Error parsing price or stop loss values in dual trade pattern: {}", message, e);
            } catch (Exception e) {
                logger.error("Unexpected error during dual trade parsing: {}", message, e);
            }
        }

        return null;
    }

    public ExitEntity[] getExitParams(String messageText) {
        if (messageText == null) {
            return null;
        }

        messageText = normalizeWhitespace(messageText);

        // Try matching each pattern in sequence
        ExitEntity[] result;

        result = matchBook100PercentProfit(messageText);
        if (result != null) {
            return result;
        }

        result = matchTrailingStopLossBookProfit(messageText);
        if (result != null) {
            return result;
        }

        result = matchTrailingStopLossTriggered(messageText);
        if (result != null) {
            return result;
        }

        result = matchBook50PercentProfit(messageText);
        if (result != null) {
            return result;
        }

        result = matchStopLossTriggered(messageText);
        if (result != null) {
            return result;
        }

        logger.debug("Message did not match any known EXIT_PATTERN.");
        return null;
    }

    private ExitEntity[] matchStopLossTriggered(String messageText) {
        String pattern = "SQUARE OFF.*?Stop loss triggered\\. Modify your stop loss and square off position\\..*?(Sell|Buy)\\s(\\d+)\\s(CE|PE)\\s@\\s(\\d+)(?:\\s.*?and\\s(Sell|Buy)\\s(\\d+)\\s(CE|PE)\\s@\\s(\\d+))?";
        Matcher matcher = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL).matcher(messageText);

        if (matcher.find()) {
            try {
                // First leg
                String action1 = matcher.group(1).toUpperCase().trim();
                String strike1 = matcher.group(2).trim();
                String optionType1 = matcher.group(3).toUpperCase().trim();
                double exitPrice1 = Double.parseDouble(matcher.group(4));

                ExitEntity exit1 = new ExitEntity(action1, strike1, exitPrice1, false, optionType1);

                // Second leg (if present)
                if (matcher.group(5) != null) {
                    String action2 = matcher.group(5).toUpperCase().trim();
                    String strike2 = matcher.group(6).trim();
                    String optionType2 = matcher.group(7).toUpperCase().trim();
                    double exitPrice2 = Double.parseDouble(matcher.group(8));

                    ExitEntity exit2 = new ExitEntity(action2, strike2, exitPrice2, false, optionType2);

                    return new ExitEntity[]{exit1, exit2};
                }

                return new ExitEntity[]{exit1};
            } catch (Exception e) {
                logger.error("Error parsing 'stop loss triggered' pattern: {}", messageText, e);
                return null;
            }
        }

        return null;
    }

    private ExitEntity[] matchTrailingStopLossBookProfit(String messageText) {
        String pattern = "SQUARE OFF.*?Trailing stop loss triggered\\. Modify stop loss and book profit for remaining 50% quantity\\..*?(Sell|Buy)\\s(\\d+)\\s(CE|PE)\\s@\\s(\\d+)(?:\\s.*?and\\s(Sell|Buy)\\s(\\d+)\\s(CE|PE)\\s@\\s(\\d+))?";
        Matcher matcher = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL).matcher(messageText);

        if (matcher.find()) {
            try {
                // First leg
                String action1 = matcher.group(1).toUpperCase().trim();
                String strike1 = matcher.group(2).trim();
                String optionType1 = matcher.group(3).toUpperCase().trim();
                double exitPrice1 = Double.parseDouble(matcher.group(4));

                ExitEntity exit1 = new ExitEntity(action1, strike1, exitPrice1, true, optionType1);

                // Second leg (if present)
                if (matcher.group(5) != null) {
                    String action2 = matcher.group(5).toUpperCase().trim();
                    String strike2 = matcher.group(6).trim();
                    String optionType2 = matcher.group(7).toUpperCase().trim();
                    double exitPrice2 = Double.parseDouble(matcher.group(8));

                    ExitEntity exit2 = new ExitEntity(action2, strike2, exitPrice2, true, optionType2);

                    return new ExitEntity[]{exit1, exit2};
                }

                return new ExitEntity[]{exit1};
            } catch (Exception e) {
                logger.error("Error parsing 'trailing stop loss book profit' pattern: {}", messageText, e);
                return null;
            }
        }

        return null;
    }

    private ExitEntity[] matchBook50PercentProfit(String messageText) {
        String pattern = "SQUARE OFF.*?Modify stop loss and book 50% profit and now keep trailing stop loss at cost for remaining 50% qty\\..*?(Sell|Buy)\\s(\\d+)\\s(CE|PE)\\s@\\s(\\d+)(?:\\s.*?and\\s(Sell|Buy)\\s(\\d+)\\s(CE|PE)\\s@\\s(\\d+))?";
        Matcher matcher = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL).matcher(messageText);

        if (matcher.find()) {
            try {
                // First leg
                String action1 = matcher.group(1).toUpperCase().trim();
                String strike1 = matcher.group(2).trim();
                String optionType1 = matcher.group(3).toUpperCase().trim();
                double exitPrice1 = Double.parseDouble(matcher.group(4));

                ExitEntity exit1 = new ExitEntity(action1, strike1, exitPrice1, true, optionType1);

                // Second leg (if present)
                if (matcher.group(5) != null) {
                    String action2 = matcher.group(5).toUpperCase().trim();
                    String strike2 = matcher.group(6).trim();
                    String optionType2 = matcher.group(7).toUpperCase().trim();
                    double exitPrice2 = Double.parseDouble(matcher.group(8));

                    ExitEntity exit2 = new ExitEntity(action2, strike2, exitPrice2, true, optionType2);

                    return new ExitEntity[]{exit1, exit2};
                }

                return new ExitEntity[]{exit1};
            } catch (Exception e) {
                logger.error("Error parsing 'book 50% profit' pattern: {}", messageText, e);
                return null;
            }
        }

        return null;
    }

    private ExitEntity[] matchBook100PercentProfit(String messageText) {
        String pattern = "SQUARE OFF.*?Modify stop loss and book 100% profit\\..*?(Sell|Buy)\\s(\\d+)\\s(CE|PE)\\s@\\s(\\d+)(?:\\s.*?and\\s(Sell|Buy)\\s(\\d+)\\s(CE|PE)\\s@\\s(\\d+))?";
        Matcher matcher = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL).matcher(messageText);

        if (matcher.find()) {
            try {
                // First leg
                String action1 = matcher.group(1).toUpperCase().trim();
                String strike1 = matcher.group(2).trim();
                String optionType1 = matcher.group(3).toUpperCase().trim();
                double exitPrice1 = Double.parseDouble(matcher.group(4));

                ExitEntity exit1 = new ExitEntity(action1, strike1, exitPrice1, false, optionType1);

                // Second leg (if present)
                if (matcher.group(5) != null) {
                    String action2 = matcher.group(5).toUpperCase().trim();
                    String strike2 = matcher.group(6).trim();
                    String optionType2 = matcher.group(7).toUpperCase().trim();
                    double exitPrice2 = Double.parseDouble(matcher.group(8));

                    ExitEntity exit2 = new ExitEntity(action2, strike2, exitPrice2, false, optionType2);

                    return new ExitEntity[]{exit1, exit2};
                }

                return new ExitEntity[]{exit1};
            } catch (Exception e) {
                logger.error("Error parsing 'book 100% profit' pattern: {}", messageText, e);
                return null;
            }
        }

        return null;
    }

    private ExitEntity[] matchTrailingStopLossTriggered(String messageText) {
        String pattern = "SQUARE OFF.*?Trailing stop loss triggered\\..*?(Sell|Buy)\\s(\\d+)\\s(CE|PE)\\s@\\s(\\d+)(?:\\s.*?and\\s(Sell|Buy)\\s(\\d+)\\s(CE|PE)\\s@\\s(\\d+))?";
        Matcher matcher = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL).matcher(messageText);

        if (matcher.find()) {
            try {
                // First leg
                String action1 = matcher.group(1).toUpperCase().trim();
                String strike1 = matcher.group(2).trim();
                String optionType1 = matcher.group(3).toUpperCase().trim();
                double exitPrice1 = Double.parseDouble(matcher.group(4));

                ExitEntity exit1 = new ExitEntity(action1, strike1, exitPrice1, false, optionType1);

                // Second leg (if present)
                if (matcher.group(5) != null) {
                    String action2 = matcher.group(5).toUpperCase().trim();
                    String strike2 = matcher.group(6).trim();
                    String optionType2 = matcher.group(7).toUpperCase().trim();
                    double exitPrice2 = Double.parseDouble(matcher.group(8));

                    ExitEntity exit2 = new ExitEntity(action2, strike2, exitPrice2, false, optionType2);

                    return new ExitEntity[]{exit1, exit2};
                }

                return new ExitEntity[]{exit1};
            } catch (Exception e) {
                logger.error("Error parsing 'trailing stop loss triggered' pattern: {}", messageText, e);
                return null;
            }
        }

        return null;
    }
}