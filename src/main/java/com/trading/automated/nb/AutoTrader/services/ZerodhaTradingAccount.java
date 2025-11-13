package com.trading.automated.nb.AutoTrader.services;

import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.User;
import com.zerodhatech.models.Quote;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("zerodhaAccount") // Mark this class as a Spring Service
public class ZerodhaTradingAccount implements ITradingAccount {

    // Note: API Key and Secret should be String type, not int, as they contain alphanumeric characters.
    @Value("${broker.api.key}")
    private String apiKey;

    @Value("${broker.api.secret}")
    private String apiSecret;

    @Value("${broker.name}")
    private String brokerName;

    @Value("${lots}")
    private int lots;

    private final String productType = "NRML";

    private static final int LOT_SIZE = 75;

    private KiteConnect kiteConnect;
    private String accessToken = null;
    private final Map<String, String> productMap = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(ZerodhaTradingAccount.class);

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

    @PostConstruct
    public void init() throws IOException {
        if (brokerName.equalsIgnoreCase("zerodha"))
            this.authenticate();
    }

    @Override
    public String getLoginUrl() {
        return kiteConnect.getLoginURL();
    }

    @Override
    public ITradingAccount authenticate() throws IOException {
        this.kiteConnect = new KiteConnect(apiKey);
        logger.info("Starting Zerodha Authentication. Login URL: {}", getLoginUrl());
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- ZERODHA AUTHENTICATION ---");
            System.out.println("1. Open the following URL in your browser:");
            System.out.println("   " + getLoginUrl());
            System.out.print("2. After logging in, paste the 'request_token' here (or type 'quit'): ");

            String requestToken = scanner.nextLine().trim();

            if (requestToken.equalsIgnoreCase("quit")) {
                logger.warn("User aborted the Zerodha login process.");
                throw new IOException("Zerodha authentication aborted by user.");
            }

            try {
                generateSession(requestToken);
                // add code to fetch the users margin details and also log how many lots can be bought with available margin assuming premium of 100 to 175 with stepup of25
                logger.info("Zerodha session generated successfully.");
                return this; // Exit the loop and return the authenticated instance
            } catch (KiteException e) {
                logger.error("KiteException during login (code: {}). Invalid token or credentials.", e.code);
                System.out.println("\n!!! Login failed. Please try again with a valid request token. !!!\n");
            } catch (IOException e) {
                logger.error("IO Error during login: {}", e.getMessage());
                System.out.println("\n!!! IO Error during login. Please check network/API details. !!!\n");
            }
        }
    }

    // Fetch the user's margin details and calculate how many lots can be bought
    public void fetchMarginDetailsAndCalculateLots() {
        try {
            // Fetch margin details using KiteConnect API
            double availableMargin = 0.0;
            try {
                Object marginsResponse = kiteConnect.getMargins("equity");
                if (marginsResponse instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> margins = (Map<String, Object>) marginsResponse;
                    availableMargin = (double) margins.get("available.cash");
                    logger.info("Available Margin: {}", availableMargin);
                } else {
                    logger.error("Unexpected response type from getMargins: {}", marginsResponse.getClass().getName());
                }
            } catch (Exception e) {
                logger.error("Error fetching margins: {}", e.getMessage(), e);
            }
            logger.info("Available Margin: {}", availableMargin);

            // Define premium range and step-up
            int[] premiumRange = {100, 125, 150, 175};
            for (int premium : premiumRange) {
                // Calculate the cost of one lot
                double costPerLot = premium * LOT_SIZE;

                // Calculate the number of lots that can be bought
                int maxLots = (int) (availableMargin / costPerLot);

                logger.info("With premium {}: Cost per lot = {}, Max lots = {}", premium, costPerLot, maxLots);
            }
        } catch (KiteException e) {
            logger.error("Error fetching margin details or calculating lots: {}", e.getMessage(), e);
        }
    }

    private void generateSession(String requestToken) throws IOException, KiteException {
        // The core logic from your original prompt:
        User user = kiteConnect.generateSession(requestToken, apiSecret);
        this.accessToken = user.accessToken;
        kiteConnect.setAccessToken(accessToken);
        System.out.println("Zerodha session generated for user: " + user.userName);
    }

    @Override
    public boolean isAuthenticated() {
        return this.accessToken != null;
    }

    @Override
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

    @Override
    public String placeOrder(String tradingSymbol, String transactionType, String optionType) throws IOException, KiteException {
        String kiteOrderType = Constants.ORDER_TYPE_MARKET;
        String kiteTransactionType = transactionType.equalsIgnoreCase("BUY") ? Constants.TRANSACTION_TYPE_BUY : Constants.TRANSACTION_TYPE_SELL;

        com.zerodhatech.models.OrderParams params = new com.zerodhatech.models.OrderParams();
        params.exchange = Constants.EXCHANGE_NFO; // Options are typically NFO
        params.tradingsymbol = tradingSymbol;
        params.transactionType = kiteTransactionType;
        params.quantity = lots * LOT_SIZE;
        params.orderType = kiteOrderType;
        params.product = Constants.PRODUCT_NRML;

        int retryCount = 0;
        while (retryCount < 3) {
            try {
                Order order = kiteConnect.placeOrder(params, Constants.VARIETY_REGULAR);
                return order.orderId;
            } catch (KiteException e) {
                if (e.message != null && e.message.contains("insufficient funds")) {
                    logger.error("Order failed due to insufficient funds. No retry will be attempted.");
                    throw e;
                }
                retryCount++;
                logger.warn("Order placement failed. Attempt {} of 3. Error: {}", retryCount, e.getMessage());
                if (retryCount == 3) {
                    logger.error("Order placement failed after 3 attempts. Giving up.");
                    throw e;
                }
            } catch (IOException e) {
                retryCount++;
                logger.warn("IO Error during order placement. Attempt {} of 3. Error: {}", retryCount, e.getMessage());
                if (retryCount == 3) {
                    logger.error("Order placement failed after 3 attempts due to IO error. Giving up.");
                    throw e;
                }
            }
        }
        throw new IllegalStateException("Unexpected state: retry loop exited without throwing an exception.");
    }

    @Override
    public String checkOrderStatus(String orderId) throws IOException, KiteException {
        List<Order> orders = kiteConnect.getOrders();
        return orders.stream()
                .filter(o -> o.orderId.equals(orderId))
                .findFirst()
                .map(o -> o.status)
                .orElse(null);
    }

    @Override
    public double getLtp(String tradingSymbol) throws IOException, KiteException {
        // 1. Resolve Method Signature Mismatch (getQuote requires String[])
        String instrumentKey = "NFO:" + tradingSymbol;
        String[] instruments = new String[]{instrumentKey}; // Corrected: Pass an array of strings

        // Use getQuote, which returns a Map of InstrumentKey to Quote objects
        Map<String, Quote> quoteData = kiteConnect.getQuote(instruments); // Corrected method call

        Quote quote = quoteData.get(instrumentKey);

        // 2. Resolve Primitive Type Nullability Issue
        // Check if the Quote object is present AND check if the lastPrice field (which is a Float/Double wrapper in the model) is not null.
        if (quote != null) {
            // The lastPrice field is automatically unboxed to the float return type
            return quote.lastPrice;
        }
        throw new IOException("LTP lookup failed or returned null for " + tradingSymbol);
    }
}