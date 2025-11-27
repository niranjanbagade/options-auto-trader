package com.trading.automated.nb.AutoTrader.services.brokers;

import com.trading.automated.nb.AutoTrader.cache.GlobalContextStore;
import com.trading.automated.nb.AutoTrader.telegram.TelegramOneToOneMessageService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ZerodhaTradeService {
    private final Map<String, String> productMap = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ZerodhaTradeService.class);
    private static final Pattern CONTRACT_PATTERN =
            Pattern.compile("([A-Z]+)\\s*(\\d+)\\s*(CE|PE)", Pattern.CASE_INSENSITIVE);

    private static final Map<Integer, String> WEEKLY_MONTH_CODE_MAP = new HashMap<>();

    static {
        for (int i = 1; i <= 9; i++) {
            WEEKLY_MONTH_CODE_MAP.put(i, String.valueOf(i));
        }
        WEEKLY_MONTH_CODE_MAP.put(10, "O");
        WEEKLY_MONTH_CODE_MAP.put(11, "N");
        WEEKLY_MONTH_CODE_MAP.put(12, "D");
    }

    public String getSymbol(String strikePrice, String expiryStr) {
        // --- 1. Parse Contract ---
        Matcher contractMatch = CONTRACT_PATTERN.matcher(strikePrice.trim());
        if (!contractMatch.matches()) {
            throw new IllegalArgumentException("Invalid contract format: " + strikePrice);
        }

        String indexName = contractMatch.group(1).toUpperCase();
        String strike = contractMatch.group(2);
        String optionType = contractMatch.group(3).toUpperCase();

        // --- 2. Use Current Year ---
        int fullYear = LocalDate.now().getYear();
        String yearTwoDigits = String.valueOf(fullYear).substring(2);

        // --- 3. Parse Expiry Date ---
        LocalDate expiryDate;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
            String fullExpiryStr = expiryStr.trim() + " " + fullYear;
            expiryDate = LocalDate.parse(fullExpiryStr, formatter);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid expiry format: " + expiryStr + ". Expected format: 'DD MMM'", e);
        }

        int day = expiryDate.getDayOfMonth();
        int month = expiryDate.getMonthValue();

        // --- 4. Determine Weekly vs. Monthly Expiry ---
        final DayOfWeek TUESDAY = DayOfWeek.TUESDAY;
        boolean isTuesday = expiryDate.getDayOfWeek() == TUESDAY;

        YearMonth yearMonth = YearMonth.of(fullYear, month);
        int lastDayOfMonth = yearMonth.lengthOfMonth();

        boolean isLastOfItsKind = (lastDayOfMonth - day) < 7;
        boolean isMonthlyExpiry = isTuesday && isLastOfItsKind;

        // --- 5. Build Symbol ---
        String tradingSymbol;
        if (isMonthlyExpiry) {
            // MONTHLY FORMAT → INDEX + YY + MON + STRIKE + CE/PE
            String monthAbbr = expiryDate.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)).toUpperCase();
            tradingSymbol = indexName + yearTwoDigits + monthAbbr + strike + optionType;
            logger.info("[INFO] Using MONTHLY format for " + expiryStr + " (Last TUESDAY).");
        } else {
            // WEEKLY FORMAT → INDEX + YY + M + DD + STRIKE + CE/PE
            String monthCode = WEEKLY_MONTH_CODE_MAP.get(month);
            if (monthCode == null) {
                throw new IllegalArgumentException("Could not determine valid month code for month " + month);
            }

            String dayOfMonthStr = String.format("%02d", day);
            tradingSymbol = indexName + yearTwoDigits + monthCode + dayOfMonthStr + strike + optionType;

            if (!isTuesday) {
                logger.info("[WARN] Expiry " + expiryStr + " is not a Tuesday. Proceeding with Weekly format.");
            }
            logger.info("[INFO] Using WEEKLY format for " + expiryStr + ".");
        }

        logger.info("[INFO] Final Symbol: " + tradingSymbol);
        return tradingSymbol;
    }


    private String calculateSha256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

        // Convert byte array to Hex String
        StringBuilder hexString = new StringBuilder();
        for (byte b : encodedhash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String generateSession(String apiKey, String apiSecret, String requestToken) throws IOException, NoSuchAlgorithmException {
        String urlString = "https://api.kite.trade/session/token";

        // 1. Calculate the Checksum (SHA-256)
        // Formula: api_key + request_token + api_secret
        String inputForChecksum = apiKey + requestToken + apiSecret;
        String checksum = calculateSha256(inputForChecksum);

        // 2. Prepare Parameters (Replace api_secret with checksum)
        String urlParameters = "api_key=" + URLEncoder.encode(apiKey, "UTF-8")
                + "&request_token=" + URLEncoder.encode(requestToken, "UTF-8")
                + "&checksum=" + checksum; // No encoding needed for hex string, but safe to do so

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        // It is good practice to set the Content-Type header
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "Java/KiteConnect");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(urlParameters.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = conn.getResponseCode();

        // 3. Handle Response
        BufferedReader in;
        if (responseCode >= 200 && responseCode < 300) {
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            // Read the error stream to see why Zerodha rejected it
            in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            String json = response.toString();
            JSONObject jsonObject = new JSONObject(json);
            // Check if "data" exists to avoid NullPointer
            if (jsonObject.has("data")) {
                return jsonObject.getJSONObject("data").getString("access_token");
            } else {
                throw new IOException("Unexpected JSON format: " + json);
            }
        } else {
            throw new IOException("Failed to generate session (HTTP " + responseCode + "). Response: " + response.toString());
        }
    }
}