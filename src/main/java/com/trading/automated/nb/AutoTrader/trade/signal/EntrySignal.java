package com.trading.automated.nb.AutoTrader.trade.signal;

import com.trading.automated.nb.AutoTrader.enums.MessagePattern;
import lombok.Data;

import java.util.regex.Pattern;

@Data
public class EntrySignal {

    private String symbol;
    private double lowerBound;
    private double upperBound;
    private double ltp;
    private String action;

    private static final String ENTRY_REGEX = "(BUY|SELL)\\s*[–-]?\\s*[0-9]{1,2}\\s*[A-Z]{3,4}\\s*[–-]?\\s*\"?([A-Z]+)\\s*(\\d+)\\s*(CE|PE)\"?\\s*(between|@|at)\\s*(\\d+\\.?\\d*)\\s*[–-]?\\s*(\\d+\\.?\\d*)";
    private static final Pattern ENTRY_PATTERN = Pattern.compile(ENTRY_REGEX,
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public MessagePattern getMessagePattern() {
        return MessagePattern.ENTRY_SIGNAL;
    }

    public EntrySignal getInstance(String signal) {
        return null;
    }
}
