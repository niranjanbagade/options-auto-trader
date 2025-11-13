package com.trading.automated.nb.AutoTrader.trade.signal;

import com.trading.automated.nb.AutoTrader.enums.MessagePattern;

public class UnknownSginal {
    public MessagePattern getMessagePattern() {
        return MessagePattern.UNKNOWN_SIGNAL;
    }

    public UnknownSginal getInstance(String signal) {
        return null;
    }
}
