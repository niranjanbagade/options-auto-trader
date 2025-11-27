package com.trading.automated.nb.AutoTrader.services.brokers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GrowwTradeService {
    private static final Logger logger = LoggerFactory.getLogger(GrowwTradeService.class);

    // Define constants for fixed components
    private static final String EXCHANGE = "NSE";
    private static final String UNDERLYING_SYMBOL = "NIFTY";

    // Pattern to extract Strike Price and Contract Type (CE/PE)
    // E.g., from "Nifty 26000 CE", it captures "26000" and "CE"
    private static final Pattern CONTRACT_PATTERN = Pattern.compile("(\\d+)\\s+(CE|PE)");
    private static final String API_URL = "https://api.groww.in/v1/token/api/access";

    /**
     * Constructs the Groww symbol for an NIFTY Options contract.
     *
     * @param inputOptionString The option description (e.g., "Nifty 26000 CE").
     * @param expiryDateString  The short expiry date (e.g., "18 Nov").
     * @return The formatted Groww Symbol string (e.g., "NSE-NIFTY-18Nov25-26000-CE").
     */
    public String getSymbol(String inputOptionString, String expiryDateString) {

        // --- 1. Extract Strike Price and Contract Type ---
        Matcher matcher = CONTRACT_PATTERN.matcher(inputOptionString);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid option string format. Expected 'Nifty [Strike] [CE/PE]'.");
        }

        // Group 1: Strike Price (e.g., "26000")
        String strikePrice = matcher.group(1);

        // Group 2: Contract Type (e.g., "CE" or "PE")
        String contractType = matcher.group(2).toUpperCase();

        // --- 2. Format Expiry Date ---
        // Assuming the current year for the expiry (a necessary assumption)
        int currentYear = LocalDate.now().getYear();

        // Create a temporary date string compatible with a standard pattern
        // e.g., "18 Nov 2025"
        String tempDateString = expiryDateString + " " + currentYear;

        // The required output format: DDMmmYY (e.g., 18Nov25)
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("d MMM yyyy");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("ddMMMuu").withLocale(java.util.Locale.ENGLISH);

        // Parse and format the date
        LocalDate expiryDate = LocalDate.parse(tempDateString, inputFormatter);
        String formattedExpiry = expiryDate.format(outputFormatter);

        // --- 3. Construct the Symbol ---
        // Format: EXCHANGE-UNDERLYING_SYMBOL-DDMmmYY-STRIKE_PRICE-OPTION_TYPE

        return String.join("-",
                EXCHANGE,
                UNDERLYING_SYMBOL,
                formattedExpiry,
                strikePrice,
                contractType);
    }

    public static String generateTotpCode(String secretKey) {
        GoogleAuthenticator gAuth = new GoogleAuthenticator();

        // This method calculates the TOTP code for the current 30-second window.
        int totpCode = gAuth.getTotpPassword(secretKey);

        // Ensure the code is 6 digits long with leading zeros if necessary
        return String.format("%06d", totpCode);
    }

    public static String getAccessToken(String userApiKey, String totpCode) throws Exception {

        // 1. Construct the JSON Request Body
        String jsonBody = String.format(
                "{\"key_type\": \"totp\", \"totp\": \"%s\"}",
                totpCode
        );

        // 2. Build the HttpClient
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // 3. Build the HttpRequest
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + userApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // 4. Send the Request
        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        // Handle API response
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            logger.info("✅ Access Token Request successful. Status Code: {}", response.statusCode());
        } else {
            logger.error("❌ Access Token Request failed. Status Code: {}. Response Body: {}", response.statusCode(), response.body());
            // Throw an exception to stop the process if the API call fails
            throw new RuntimeException("API call failed with status code: " + response.statusCode());
        }

        return response.body();
    }

    public static TokenResponse parseGrowwResponse(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Read the JSON string and map it directly to the POJO class
            TokenResponse response = objectMapper.readValue(jsonString, TokenResponse.class);

            return response;

        } catch (Exception e) {
            logger.error("Error parsing JSON response: {}", e.getMessage(), e);
            return null; // Handle error appropriately
        }
    }

    public String generateSession(String apiKey, String secretKey) {
        String totpCode = generateTotpCode(secretKey);
        try {
            String accessTokenResponse = getAccessToken(apiKey, totpCode);
            TokenResponse parsedData = parseGrowwResponse(accessTokenResponse);
            return parsedData == null ? null : parsedData.getToken();
        } catch (Exception e) {
            throw new RuntimeException("Error generating session: " + e.getMessage(), e);
        }
    }


    public static class TokenResponse {

        // The @JsonProperty annotation is crucial if your Java variable name
        // doesn't exactly match the JSON key (though here they do).
        // It's good practice for clarity.

        @JsonProperty("token")
        private String token; // This holds the actual JWT

        @JsonProperty("tokenRefId")
        private String tokenRefId;

        @JsonProperty("sessionName")
        private String sessionName;

        @JsonProperty("expiry")
        private String expiry;

        @JsonProperty("active")
        private boolean active;

        // Default constructor is required by Jackson
        public TokenResponse() {
        }

        // Getter for the access token
        public String getToken() {
            return token;
        }

        // Setter (required by Jackson for deserialization)
        public void setToken(String token) {
            this.token = token;
        }

        // You can add getters/setters for other fields if you need them
        // ...

        @Override
        public String toString() {
            return "TokenResponse{" +
                    "token='" + (token != null ? token.substring(0, 15) + "..." : "null") + '\'' +
                    ", tokenRefId='" + tokenRefId + '\'' +
                    '}';
        }
    }
}
