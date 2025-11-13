package com.trading.automated.nb.AutoTrader.broker.factory;

import com.trading.automated.nb.AutoTrader.enums.BrokerType;
import com.trading.automated.nb.AutoTrader.services.ITradingAccount;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TradingAccountFactory {

    private final Map<String, ITradingAccount> accountMap;

    // Spring injects ALL beans implementing ITradingAccount into this Map, 
    // using the @Service name as the key (e.g., "zerodhaAccount", "fyersAccount")
    public TradingAccountFactory(Map<String, ITradingAccount> accountMap) {
        this.accountMap = accountMap;
    }

    public ITradingAccount getTradingAccount(BrokerType type) {
        String key = type.name().toLowerCase() + "Account"; // Constructs "zerodhaAccount", etc.
        
        ITradingAccount account = accountMap.get(key);
        
        if (account == null) {
            throw new IllegalArgumentException("Unsupported broker type: " + type);
        }
        return account;
    }
}