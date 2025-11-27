package com.trading.automated.nb.AutoTrader.services.master.follower;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
public class GrowwFollowerService {

    private static final Logger logger = LoggerFactory.getLogger(GrowwFollowerService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    // Placeholder for Groww Order API URL
    private static final String GROWW_ORDER_URL = "https://api.groww.in/v1/order/create";

    @Autowired
    private TelegramOneToOneMessageService telegramService;

    // Retryable HTTP codes are common for rate limits/server issues
    private static final Set<Integer> RETRYABLE_HTTP_CODES = Set.of(429, 503, 504);
    private static final String[] RETRYABLE_ERROR_MESSAGES = {
            "timeout", "temporarily unavailable", "Too many requests", "rate limit"
    };

    @Autowired
    GlobalContextStore globalContextStore;

    @Async("asyncExecutor")
    @Retryable(
            value = {RetryableOrderException.class, SocketTimeoutException.class, IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2) // Exponential (1s, 2s) between attempts
    )
    public CompletableFuture<Boolean> placeOrderAsync(
            String tradingSymbol, String optionType, String action, boolean isSquareOff, UnifiedClientData account, String accessToken) {
        final String clientName = account.getClientName();
        final String key = clientName + "_" + tradingSymbol;
        // 2. Order Placement API Call
        HttpURLConnection conn = null;
        try {
            URL url = new URL(GROWW_ORDER_URL);
            conn = (HttpURLConnection) url.openConnection();

            // --- API Request Setup ---
            conn.setRequestMethod("POST");
            // Groww specific headers - PLACEHOLDERS
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken); // Groww uses Bearer token
            conn.setDoOutput(true);

            String exchange = "NSE";
            String orderType = "MARKET";
            String product = "NRML"; // Assuming a product type like NORMAL/MIS
            String validity = "DAY";
            int quantity = account.getLots() * 75; // Standard lot size assumption
            String tag = "AutoFNOJava";
            String segment = "FNO";
            String orderReferenceId = "JAVA-" + System.currentTimeMillis();

            // --- Construct URL Parameters (Form data) ---
            JSONObject params = new JSONObject();
            params.put("exchange", exchange);
            params.put("trading_symbol", tradingSymbol);
            params.put("transaction_type", action);
            params.put("order_type", orderType);
            params.put("quantity", quantity);
            params.put("product", product);
            params.put("validity", validity);
            params.put("tag", tag);
            params.put("segment", segment);
            params.put("order_reference_id", orderReferenceId);
            // Add any other necessary Groww parameters here (e.g., price, disclosed quantity, etc.)

            // Write parameters to the request body
            try (OutputStream os = conn.getOutputStream()) {
                os.write(params.toString().getBytes(StandardCharsets.UTF_8));
            }

            // --- Handle Response ---
            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                InputStream inputStream = conn.getInputStream();
// Read the full response body
                try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // Log or parse the response string
                    String finalResponse = response.toString();
                    logger.info("HTTP Status Code: {}", responseCode);
                    logger.info("Response Body: {}", finalResponse);

                    // Map the raw JSON string to the Java object
                    GrowwOrderResponse growwResponse = MAPPER.readValue(finalResponse, GrowwOrderResponse.class);

                    String clientMessage = getMessageFromResponse(
                            growwResponse.getPayload().getOrderStatus(),
                            growwResponse.getPayload().getRemark(),
                            growwResponse.getPayload().getGrowwOrderId(),
                            tradingSymbol,
                            action,
                            growwResponse.getPayload().getFilledQuantity()
                    );

                    telegramService.sendMessage(account.getTelegramChannelId(),
                            clientMessage, MessageImportance.GOOD);
                    if (isSquareOff) {
                        globalContextStore.removeKey(key);
                    } else {
                        globalContextStore.setValue(key, action);
                    }
                } catch (IOException e) {
                    // Handle reading error
                    logger.info("Error reading response body ", e);
                }
                return CompletableFuture.completedFuture(true);
            } else {
                String errorMessage = "";
                // Attempt to read error stream for details
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String inputLine;
                    StringBuilder error = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        error.append(inputLine);
                    }
                    // Attempt to parse JSON error message
                    try {
                        JSONObject errorJson = new JSONObject(error.toString());
                        if (errorJson.has("error")) { // Check for common 'error' field too
                            errorMessage = errorJson.getJSONObject("error").getString("message");
                        }
                        logger.error("Order placement failed for {} {}. HTTP {} Response: {}",
                                action, tradingSymbol, responseCode, errorMessage);
                        telegramService.sendMessage(account.getTelegramChannelId(),
                                "Order placement failed for " + action + " " + tradingSymbol +
                                        ". HTTP " + responseCode + " Response: " + errorMessage, MessageImportance.MEDIUM);
                    } catch (Exception ex) {
                        logger.error("Failed to parse error message from response: {}", error, ex);
                        errorMessage = "Unknown API Error";
                    }
                }

                // --- Retry Check ---
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
        } catch (SocketTimeoutException e) {
            logger.error("Socket timeout during order placement", e);
            throw new RetryableOrderException("Socket timeout", e);
        } catch (IOException e) {
            logger.error("IO/network error during order placement", e);
            throw new RetryableOrderException("Network IO error", e);
        } catch (Exception e) {
            logger.error("Order placement failed - not retrying", e);
            return CompletableFuture.completedFuture(false);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public String getMessageFromResponse(String orderStatus, String remark, String orderId,
                                         String tradingSymbol, String action, Integer filledQuantity) {

        StringBuilder sb = new StringBuilder();

        String status = orderStatus != null ? orderStatus : "UNKNOWN";

        if ("NEW".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status)) {
            sb.append("🔔 **ORDER ACKNOWLEDGED (Processing)**\n\n")
                    .append("Action: ").append(action).append(" ").append(tradingSymbol).append("\n")
                    .append("Status: Acknowledged by Groww system (`").append(status).append("`)\n")
                    .append("Groww ID: `").append(orderId).append("`\n")
                    .append("Note: Status will update to FILLED or REJECTED shortly. Keep an eye on your account.");
        } else if ("REJECTED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
            sb.append("❌ **ORDER REJECTED**\n\n")
                    .append("Action: ").append(action).append(" ").append(tradingSymbol).append("\n")
                    .append("Status: **REJECTED**\n")
                    .append("Reason: **").append(remark != null ? remark : "Unknown").append("**\n")
                    .append("Groww ID: `").append(orderId).append("`\n")
                    .append("Check account permissions (e.g., FNO segment active).");
        } else if ("FILLED".equalsIgnoreCase(status)) {
            sb.append("✅ **ORDER FILLED (Instant)**\n\n")
                    .append("Action: ").append(action).append(" ").append(tradingSymbol).append("\n")
                    .append("Status: **FILLED**\n")
                    .append("Quantity: ").append(filledQuantity != null ? filledQuantity : "N/A").append("\n")
                    .append("Groww ID: `").append(orderId).append("`");
        } else {
            sb.append("⚠️ **ORDER STATUS UNKNOWN**\n\n")
                    .append("Action: ").append(action).append(" ").append(tradingSymbol).append("\n")
                    .append("Status: `").append(status).append("`\n")
                    .append("Groww ID: `").append(orderId).append("`");
        }

        return sb.toString();
    }

    // When retries are exhausted
    @Recover
    public CompletableFuture<Boolean> recover(RetryableOrderException e,
                                              String tradingSymbol, String optionType, String action, boolean isSquareOff, UnifiedClientData account, String accessToken) {
        logger.error("Retries exhausted for order placement for {} {}: {}", action, tradingSymbol, e.getMessage());
        telegramService.sendMessage(account.getTelegramChannelId(),
                "Order placement failed after retries for " + action + " " + tradingSymbol +
                        ". Error: " + e.getMessage(), MessageImportance.HIGH);
        return CompletableFuture.completedFuture(false);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GrowwOrderResponse {
        // Top-level API status (SUCCESS or FAILURE)
        private String status;

        // The actual order data object
        private OrderStatusPayload payload;

        // Standard Getters and Setters (omitted for brevity, but needed by Jackson)
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public OrderStatusPayload getPayload() {
            return payload;
        }

        public void setPayload(OrderStatusPayload payload) {
            this.payload = payload;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderStatusPayload {

        @JsonProperty("groww_order_id")
        private String growwOrderId;

        @JsonProperty("order_status")
        // This is the CRITICAL field (e.g., REJECTED, OPEN, FILLED)
        private String orderStatus;

        @JsonProperty("remark")
        // This will contain the REJECTION REASON (e.g., "FNO segment not active")
        private String remark;

        @JsonProperty("filled_quantity")
        private Integer filledQuantity;

        @JsonProperty("order_reference_id")
        private String orderReferenceId;

        // Standard Getters and Setters (omitted for brevity, but needed by Jackson)
        public String getGrowwOrderId() {
            return growwOrderId;
        }

        public void setGrowwOrderId(String growwOrderId) {
            this.growwOrderId = growwOrderId;
        }

        public String getOrderStatus() {
            return orderStatus;
        }

        public void setOrderStatus(String orderStatus) {
            this.orderStatus = orderStatus;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public Integer getFilledQuantity() {
            return filledQuantity;
        }

        public void setFilledQuantity(Integer filledQuantity) {
            this.filledQuantity = filledQuantity;
        }

        public String getOrderReferenceId() {
            return orderReferenceId;
        }

        public void setOrderReferenceId(String orderReferenceId) {
            this.orderReferenceId = orderReferenceId;
        }
    }
}