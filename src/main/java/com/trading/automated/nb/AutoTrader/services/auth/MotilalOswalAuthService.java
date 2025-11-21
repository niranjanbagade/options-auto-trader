package com.trading.automated.nb.AutoTrader.services.auth;

import com.trading.automated.nb.AutoTrader.config.MotilalTradingConfig;
import jakarta.annotation.PostConstruct;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.jboss.aerogear.security.otp.Totp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MotilalOswalAuthService {

    private static final Logger logger = LoggerFactory.getLogger(MotilalOswalAuthService.class);
    
    private static final String LOGIN_URL = "https://openapi.motilaloswal.com/rest/login/v3/authdirectapi";
    private HttpClient httpClient;
    @Autowired
    @Qualifier("motilalTradingConfig")
    private MotilalTradingConfig tradingConfig;

    // Thread-safe cache to store tokens: <ClientCode, AuthToken>
    private final Map<String, String> tokenCache = new ConcurrentHashMap<>();

    public MotilalOswalAuthService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

//    @PostConstruct
//    public void initTokens() {
//        logger.info(">>> Application Startup: generating initial tokens...");
//        refreshAllTokens();
//    }

//    @Scheduled(fixedRate = 300000)
    public void refreshAllTokens() {
        if (tradingConfig.getAccounts() == null) return;

        logger.info(">>> Scheduled Task: Refreshing tokens for all users...");

        for (MotilalTradingConfig.MotilalOswalAccount user : tradingConfig.getAccounts()) {
            try {
                String newToken = loginAndGetToken(user);
                tokenCache.put(user.getClientCode(), newToken);
                logger.info("✔ Token refreshed for: " + user.getClientCode());
            } catch (Exception e) {
                System.err.println("✘ Failed to refresh token for " + user.getClientCode() + ": " + e.getMessage());
            }
        }
    }

    public String getAuthToken(MotilalTradingConfig.MotilalOswalAccount user) {
        if (tokenCache.containsKey(user.getClientCode())) {
            return tokenCache.get(user.getClientCode());
        }

        try {
            String token = loginAndGetToken(user);
            tokenCache.put(user.getClientCode(), token);
            return token;
        } catch (Exception e) {
            throw new RuntimeException("Login failed for user " + user.getClientCode(), e);
        }
    }

    private String loginAndGetToken(MotilalTradingConfig.MotilalOswalAccount user) throws Exception {
        Totp totpGenerator = new Totp(user.getTotpSecret());
        String currentOtp = totpGenerator.now();

        String rawString = user.getPassword() + user.getApiKey();
        String passwordHash = DigestUtils.sha256Hex(rawString);

        String loginJson = String.format("{"
                        + "\"userid\": \"%s\","
                        + "\"password\": \"%s\","
                        + "\"2FA\": \"%s\","
                        + "\"totp\": \"%s\""
                        + "}",
                user.getClientCode(), passwordHash, user.getDob(), currentOtp);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_URL))
                .header("Content-Type", "application/json")
                .header("ApiKey", user.getApiKey())
                .header("User-Agent", "SpringBoot-Bot/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();

        if (response.statusCode() == 200 && body.contains("AuthToken")) {
            return body.split("\"AuthToken\":\"")[1].split("\"")[0];
        } else {
            throw new RuntimeException("Response: " + body);
        }
    }
}
