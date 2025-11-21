package com.trading.automated.nb.AutoTrader.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsentData {
    private String email;
    private String broker; // Zerodha, Motilaloswal, Grow
    private String requestToken; // Only if Zerodha
    private boolean consentGiven;
    private LocalDateTime timestamp;

    private int lots;

    // Constructors
    public ConsentData() {}

    public ConsentData(String email, String broker, String requestToken, boolean consentGiven, LocalDateTime timestamp, int lots) {
        this.email = email;
        this.broker = broker;
        this.requestToken = requestToken;
        this.consentGiven = consentGiven;
        this.timestamp = timestamp;
        this.lots = lots%2==0 ? lots : lots -1;
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBroker() { return broker; }
    public void setBroker(String broker) { this.broker = broker; }

    public String getRequestToken() { return requestToken; }
    public void setRequestToken(String requestToken) { this.requestToken = requestToken; }

    public boolean isConsentGiven() { return consentGiven; }
    public void setConsentGiven(boolean consentGiven) { this.consentGiven = consentGiven; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getLots() {
        return lots;
    }

    public void setLots(int lots) {
        this.lots = lots%2==0 ? lots : lots -1;
    }

    @Override
    public String toString() {
        return "ConsentData{" +
                "email='" + email + '\'' +
                ", broker='" + broker + '\'' +
                ", requestToken='" + requestToken + '\'' +
                ", consentGiven=" + consentGiven +
                ", timestamp=" + timestamp +
                '}';
    }
}