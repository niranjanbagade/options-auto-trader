package com.trading.automated.nb.AutoTrader.broker.factory;

import com.trading.automated.nb.AutoTrader.enums.BrokerType;
import com.trading.automated.nb.AutoTrader.services.TradingAccount;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TradingAccountFactory {

    private final Map<String, TradingAccount> accountMap;

    // Spring injects ALL beans implementing ITradingAccount into this Map, 
    // using the @Service name as the key (e.g., "zerodhaAccount", "fyersAccount")
    public TradingAccountFactory(Map<String, TradingAccount> accountMap) {
        this.accountMap = accountMap;
    }

    public TradingAccount getTradingAccount(BrokerType type) {
        String key = type.name().toLowerCase() + "Account"; // Constructs "zerodhaAccount", etc.
        
        TradingAccount account = accountMap.get(key);
        
        if (account == null) {
            throw new IllegalArgumentException("Unsupported broker type: " + type);
        }
        return account;
    }
}