package com.trading.automated.nb.AutoTrader.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsentData {
    private String email;
    private String tokenOrTotpKey;
    private boolean consentGiven;
    private LocalDateTime timestamp;
    private int lots;

    // Constructors
    public ConsentData() {
    }

    public ConsentData(String email, String tokenOrTotpKey, boolean consentGiven, LocalDateTime timestamp, int lots) {
        this.email = email;
        this.tokenOrTotpKey = tokenOrTotpKey;
        this.consentGiven = consentGiven;
        this.timestamp = timestamp;
        setLots(lots);
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTokenOrTotpKey() {
        return tokenOrTotpKey;
    }

    public void setTokenOrTotpKey(String tokenOrTotpKey) {
        this.tokenOrTotpKey = tokenOrTotpKey;
    }

    public boolean isConsentGiven() {
        return consentGiven;
    }

    public void setConsentGiven(boolean consentGiven) {
        this.consentGiven = consentGiven;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getLots() {
        return lots;
    }

    public void setLots(int lots) {
        if (lots < 0) {
            this.lots = 0;
        } else {
            this.lots = (lots % 2 == 0) ? lots : lots - 1;
        }
    }

    @Override
    public String toString() {
        return "ConsentData{" +
                "email='" + email + '\'' +
                ", requestToken='" + tokenOrTotpKey + '\'' +
                ", consentGiven=" + consentGiven +
                ", timestamp=" + timestamp +
                '}';
    }
}