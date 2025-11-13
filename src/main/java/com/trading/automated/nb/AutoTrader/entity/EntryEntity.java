package com.trading.automated.nb.AutoTrader.entity;


public class EntryEntity {
    private String action;
    private String expiry;
    private String strikePrice;

    private String optionType;
    private double priceA;
    private double priceB;

    public EntryEntity(String action, String expiry, String strikePrice, String optionType, double priceA, double priceB) {
        this.action = action;
        this.expiry = expiry;
        this.strikePrice = strikePrice;
        this.optionType = optionType;
        this.priceA = priceA;
        this.priceB = priceB;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getExpiry() {
        return expiry;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }

    public String getStrikePrice() {
        return strikePrice;
    }

    public void setStrikePrice(String strikePrice) {
        this.strikePrice = strikePrice;
    }

    public double getPriceA() {
        return priceA;
    }

    public void setPriceA(double priceA) {
        this.priceA = priceA;
    }

    public double getPriceB() {
        return priceB;
    }

    public void setPriceB(double priceB) {
        this.priceB = priceB;
    }

    public double getLowerBound(){
        return Math.min(getPriceA(), getPriceB());
    }

    public double getUpperBound(){
        return Math.max(getPriceA(), getPriceB());
    }

    public String getOptionType() {
        return optionType;
    }

    public void setOptionType(String optionType) {
        this.optionType = optionType;
    }
}
