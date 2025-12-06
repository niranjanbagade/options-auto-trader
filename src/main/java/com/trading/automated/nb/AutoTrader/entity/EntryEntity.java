package com.trading.automated.nb.AutoTrader.entity;

import lombok.Data;

@Data
public class EntryEntity {
    private String action;
    private String expiry;
    private String strike;
    private String optionType;
    private double priceA;
    private double priceB;
    private double stopLoss;

    public EntryEntity(String action, String expiry, String strike, String optionType, double priceA, double priceB,
            double stopLoss) {
        this.action = action;
        this.expiry = expiry;
        this.strike = strike;
        this.optionType = optionType;
        this.priceA = priceA;
        this.priceB = priceB;
        this.stopLoss = stopLoss;
    }
}