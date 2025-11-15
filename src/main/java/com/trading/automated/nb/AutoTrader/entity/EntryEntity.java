package com.trading.automated.nb.AutoTrader.entity;

import org.springframework.cache.annotation.Cacheable;

public class EntryEntity {
    private String action;
    private String expiry;
    private String strike;
    private String optionType;
    private double priceA;
    private double priceB;
    private double stopLoss;

    public EntryEntity(String action, String expiry, String strike, String optionType, double priceA, double priceB, double stopLoss) {
        this.action = action;
        this.expiry = expiry;
        this.strike = strike;
        this.optionType = optionType;
        this.priceA = priceA;
        this.priceB = priceB;
        this.stopLoss = stopLoss;
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

    public String getStrike() {
        return strike;
    }

    public void setStrike(String strike) {
        this.strike = strike;
    }

    public String getOptionType() {
        return optionType;
    }

    public void setOptionType(String optionType) {
        this.optionType = optionType;
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

    public double getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(double stopLoss) {
        this.stopLoss = stopLoss;
    }
}