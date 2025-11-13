package com.trading.automated.nb.AutoTrader.services;

import com.zerodhatech.models.Order;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import java.io.IOException;

public interface ITradingAccount {
    // Authenticates and returns the authenticated instance.
    ITradingAccount authenticate() throws IOException, KiteException;

    // Returns the Zerodha login URL for the initial request token.
    String getLoginUrl();

    void fetchMarginDetailsAndCalculateLots();

    // Places an order and returns the Order ID.
    String placeOrder(String tradingSymbol, String transactionType, String orderType) throws IOException, KiteException;

    // Checks the status of a specific order ID.
    String checkOrderStatus(String orderId) throws IOException, KiteException;

    // Retrieves the Last Traded Price (LTP) of a symbol.
    double getLtp(String tradingSymbol) throws IOException, KiteException;

    // Checks if the session is currently authenticated.
    boolean isAuthenticated();

    public String getSymbol(String strikePrice, String expiryStr);
}
