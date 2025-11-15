package com.trading.automated.nb.AutoTrader.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// By default, Spring beans are singletons, meaning only one instance exists.
@Component 
public class GlobalContextStore {

    private static final Logger logger = LoggerFactory.getLogger(GlobalContextStore.class);
    private final Map<String, String> contextData = new ConcurrentHashMap<>();

    public void setValue(String key, String value) {
        logger.info("Setting context data: " + key + " = " + value);
        contextData.put(key, value);
    }

    public void removeKey(String key) {
        logger.info("Removing context data for key: " + key);
        contextData.remove(key);
    }

    public void clearAll() {
        logger.info("Clearing all context data.");
        contextData.clear();
    }

    public boolean containsKey(String key) {
        logger.info("Checking presence of key: " + key);
        return contextData.containsKey(key);
    }

    public String getValue(String key) {
        return contextData.get(key);
    }
    
    public Map<String, String> getAllValues() {
        return contextData;
    }
}