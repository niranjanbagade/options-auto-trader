package com.trading.automated.nb.AutoTrader.dtos;

// Simple DTO for clean service method usage
public class OrderDto {
    private String tradingSymbol; // e.g., "RELIANCE"
    private String transactionType; // e.g., "BUY", "SELL"
    private String orderType;     // e.g., "LIMIT", "MARKET", "STOPLOSS"
    private double price;         // The price for LIMIT/STOPLOSS
    private int quantity;         // Quantity in lots or shares

    // Constructor, Getters, and Setters
    public OrderDto(String tradingSymbol, String transactionType, String orderType, double price, int quantity) {
        this.tradingSymbol = tradingSymbol;
        this.transactionType = transactionType;
        this.orderType = orderType;
        this.price = price;
        this.quantity = quantity;
    }

    public String getTradingSymbol() { return tradingSymbol; }
    public void setTradingSymbol(String tradingSymbol) { this.tradingSymbol = tradingSymbol; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    @Override
    public String toString() {
        return "OrderDto{" +
                "symbol='" + tradingSymbol + '\'' +
                ", type='" + transactionType + '\'' +
                ", orderType='" + orderType + '\'' +
                ", price=" + price +
                ", qty=" + quantity +
                '}';
    }
}