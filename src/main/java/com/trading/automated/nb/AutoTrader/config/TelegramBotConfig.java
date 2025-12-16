package com.trading.automated.nb.AutoTrader.config;

import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;

@Configuration
public class TelegramBotConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(TelegramBotConfig.class);

    @Bean
    public DefaultBotOptions telegramDefaultBotOptions(
            @Value("${telegram.proxy.type:NO_PROXY}") String proxyType,
            @Value("${telegram.proxy.host:}") String proxyHost,
            @Value("${telegram.proxy.port:0}") int proxyPort) {
        
        DefaultBotOptions options = new DefaultBotOptions();
        
        // Use shorter, cleaner names for timeouts
        final int TIMEOUT_MS = 15000; 

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT_MS)
                .setSocketTimeout(TIMEOUT_MS)
                .build();
        options.setRequestConfig(requestConfig);

        if (proxyType != null && !proxyType.equalsIgnoreCase("NO_PROXY")) {
            try {
                options.setProxyType(DefaultBotOptions.ProxyType.valueOf(proxyType.toUpperCase()));
                options.setProxyHost(proxyHost);
                options.setProxyPort(proxyPort);
                logger.info("Configured Telegram Bot to use Proxy: Type={}, Host={}, Port={}", proxyType, proxyHost, proxyPort);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid Proxy Type configured: {}. Proceeding without proxy.", proxyType);
            }
        }
        
        return options;
    }
}