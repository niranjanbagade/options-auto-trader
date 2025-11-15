package com.trading.automated.nb.AutoTrader.services;

import com.trading.automated.nb.AutoTrader.telegram.TelegramAckService;
import com.zerodhatech.models.Order;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

public abstract class TradingAccount {
    @Value("${broker.api.key}")
    protected String apiKey;

    @Value("${broker.api.secret}")
    protected String apiSecret;

    @Value("${broker.name}")
    protected String brokerName;

    @Value("${lots}")
    protected int lots;

    @Value("${broker.testing:false}")
    protected boolean isTesting;

    @Autowired
    protected TelegramAckService telegramAckService;

    protected static final int LOT_SIZE = 75;
    // Authenticates and returns the authenticated instance.
    public abstract void authenticate() throws IOException, KiteException;

    public abstract void startSessionKeepAlive();

    // Returns the Zerodha login URL for the initial request token.
    public abstract String getLoginUrl();

    public abstract void fetchMarginDetailsAndCalculateLots();

    // Places an order and returns the Order ID.
    public abstract String placeOrder(String tradingSymbol, String transactionType, String orderType, boolean isSquareOff) throws IOException, KiteException;

    // Checks the status of a specific order ID.
    public abstract String checkOrderStatus(String orderId) throws IOException, KiteException;

    // Retrieves the Last Traded Price (LTP) of a symbol.
    public abstract double getLtp(String tradingSymbol) throws IOException, KiteException;

    // Checks if the session is currently authenticated.
    public abstract boolean isAuthenticated();

    public abstract String getSymbol(String strikePrice, String expiryStr);
}
