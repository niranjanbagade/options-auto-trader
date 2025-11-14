package com.trading.automated.nb.AutoTrader.entity;

public class ExitEntity {
    private String action;
    private String strike;
    private double exitPrice;
    private boolean partialExit;
    private String optionType; // New field for option type

    // Updated constructor
    public ExitEntity(String action, String strike, double exitPrice, boolean partialExit, String optionType) {
        this.action = action;
        this.strike = strike;
        this.exitPrice = exitPrice;
        this.partialExit = partialExit;
        this.optionType = optionType;
    }

    // Getters and setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getStrike() {
        return strike;
    }

    public void setStrike(String strike) {
        this.strike = strike;
    }

    public double getExitPrice() {
        return exitPrice;
    }

    public void setExitPrice(double exitPrice) {
        this.exitPrice = exitPrice;
    }

    public boolean isPartialExit() {
        return partialExit;
    }

    public void setPartialExit(boolean partialExit) {
        this.partialExit = partialExit;
    }

    public String getOptionType() {
        return optionType;
    }

    public void setOptionType(String optionType) {
        this.optionType = optionType;
    }

    @Override
    public String toString() {
        return "ExitEntity{" +
                "action='" + action + '\'' +
                ", strike='" + strike + '\'' +
                ", exitPrice=" + exitPrice +
                ", partialExit=" + partialExit +
                ", optionType='" + optionType + '\'' +
                '}';
    }
}