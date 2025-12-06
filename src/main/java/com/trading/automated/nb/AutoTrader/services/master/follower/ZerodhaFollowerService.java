package com.trading.automated.nb.AutoTrader.services.master.follower;

import com.trading.automated.nb.AutoTrader.cache.GlobalContextStore;
import com.trading.automated.nb.AutoTrader.dtos.UnifiedClientData;
import com.trading.automated.nb.AutoTrader.enums.MessageImportance;
import com.trading.automated.nb.AutoTrader.exceptions.RetryableOrderException;
import com.trading.automated.nb.AutoTrader.telegram.TelegramOneToOneMessageService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
public class ZerodhaFollowerService {

    private static final Logger logger = LoggerFactory.getLogger(ZerodhaFollowerService.class);

    private static final String ZERODHA_ORDER_URL = "https://api.kite.trade/orders/regular";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Autowired
    private TelegramOneToOneMessageService telegramService;
    private static final Set<Integer> RETRYABLE_HTTP_CODES = Set.of(429, 503, 504);
    private static final String[] RETRYABLE_ERROR_MESSAGES = {
            "timeout", "temporarily unavailable", "Too many requests", "rate limit"
    };

    @Autowired
    GlobalContextStore globalContextStore;

    @Async("asyncExecutor")
    @Retryable(value = { RetryableOrderException.class, SocketTimeoutException.class,
            IOException.class }, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2) // Exponential (1s,
                                                                                                  // 2s) between
                                                                                                  // attempts
    )
    public CompletableFuture<Boolean> placeOrderAsync(
            String tradingSymbol, String optionType, String action, boolean isSquareOff, UnifiedClientData account,
            String accessToken) {
        final String clientName = account.getClientName();
        final String key = clientName + "_" + tradingSymbol + "_" + optionType;

        try {
            String urlString = "https://api.kite.trade/orders/regular";
            String apiKey = account.getApiKey();
            HttpURLConnection conn = null;

            try {
                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-Kite-Version", "3");
                conn.setRequestProperty("Authorization", "token " + apiKey + ":" + accessToken);
                conn.setDoOutput(true);

                String exchange = "NFO";
                String orderType = "MARKET";
                String product = "NRML";
                String validity = "DAY";
                int quantity = account.getLots() * 75;
                String tag = "AutoFNOJava";

                StringBuilder params = new StringBuilder();
                params.append("variety=").append(URLEncoder.encode("regular", "UTF-8"));
                params.append("&exchange=").append(URLEncoder.encode(exchange, "UTF-8"));
                params.append("&tradingsymbol=").append(URLEncoder.encode(tradingSymbol, "UTF-8"));
                params.append("&transaction_type=").append(URLEncoder.encode(action, "UTF-8"));
                params.append("&order_type=").append(URLEncoder.encode(orderType, "UTF-8"));
                params.append("&quantity=").append(quantity);
                params.append("&product=").append(URLEncoder.encode(product, "UTF-8"));
                params.append("&validity=").append(URLEncoder.encode(validity, "UTF-8"));
                params.append("&disclosed_quantity=0");
                params.append("&tag=").append(URLEncoder.encode(tag, "UTF-8"));

                OutputStream os = conn.getOutputStream();
                os.write(params.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    telegramService.sendMessage(account.getTelegramChannelId(),
                            "Order placed successfully: " + action + " " + tradingSymbol, MessageImportance.GOOD);
                    if (isSquareOff) {
                        globalContextStore.removeKey(key);
                    } else {
                        globalContextStore.setValue(key, action);
                    }
                    return CompletableFuture.completedFuture(true);
                } else {
                    String errorMessage = "";
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                        String inputLine;
                        StringBuilder error = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            error.append(inputLine);
                        }
                        try {
                            JSONObject errorJson = new JSONObject(error.toString());
                            if (errorJson.has("message")) {
                                errorMessage = errorJson.getString("message");
                                logger.error("Order placement failed for {} {}. HTTP {} Response: {}",
                                        action, tradingSymbol, responseCode, errorMessage);
                                telegramService.sendMessage(account.getTelegramChannelId(),
                                        "Order placement failed for " + action + " " + tradingSymbol +
                                                ". HTTP " + responseCode + " Response: " + errorMessage,
                                        MessageImportance.MEDIUM);
                            }
                        } catch (Exception ex) {
                            logger.error("Failed to parse error message from response: {}", error, ex);
                        }
                    }
                    // Retry logic: HTTP code or error message is retryable
                    boolean shouldRetry = RETRYABLE_HTTP_CODES.contains(responseCode);
                    for (String substr : RETRYABLE_ERROR_MESSAGES) {
                        if (errorMessage != null && errorMessage.toLowerCase().contains(substr.toLowerCase())) {
                            shouldRetry = true;
                            break;
                        }
                    }
                    if (shouldRetry) {
                        throw new RetryableOrderException("Retryable error: HTTP " + responseCode + " " + errorMessage);
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                }
            } finally {
                if (conn != null)
                    conn.disconnect();
            }
        } catch (SocketTimeoutException e) {
            logger.error("Socket timeout during order placement", e);
            throw new RetryableOrderException("Socket timeout", e);
        } catch (IOException e) {
            logger.error("IO/network error during order placement", e);
            throw new RetryableOrderException("Network IO error", e);
        } catch (Exception e) {
            logger.error("Order placement failed - not retrying", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    // When retries are exhausted
    @Recover
    public CompletableFuture<Boolean> recover(RetryableOrderException e,
            String tradingSymbol, String action, boolean isSquareOff, UnifiedClientData account) {
        logger.error("Retries exhausted for order placement for {} {}: {}", action, tradingSymbol, e.getMessage());
        telegramService.sendMessage(account.getClientName(),
                "Order placement failed after retries for " + action + " " + tradingSymbol +
                        ". Error: " + e.getMessage(),
                MessageImportance.HIGH);
        return CompletableFuture.completedFuture(false);
    }
}