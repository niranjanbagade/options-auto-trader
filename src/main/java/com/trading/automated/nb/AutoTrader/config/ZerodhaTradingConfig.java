package com.trading.automated.nb.AutoTrader.config;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "zerodha")
public class ZerodhaTradingConfig {
    private List<ZerodhaAccount> accounts;

    public List<ZerodhaAccount> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<ZerodhaAccount> accounts) {
        this.accounts = accounts;
    }

    public static class ZerodhaAccount {
        private String key;
        private String secret;
        private String clientTelegramId;
        private String clientName;
        private Boolean onlyBuy;
        private String email;
        private String phoneNumber;
        private boolean isFirstRequest = true;
        private int lots;
        private String accessToken;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getClientTelegramId() {
            return clientTelegramId;
        }

        public void setClientTelegramId(String clientTelegramId) {
            this.clientTelegramId = clientTelegramId;
        }

        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        public Boolean getOnlyBuy() {
            return onlyBuy;
        }

        public void setOnlyBuy(Boolean onlyBuy) {
            this.onlyBuy = onlyBuy;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public int getLots() {
            return lots;
        }

        public void setLots(int lots) {
            this.lots = lots;
        }
    }
}
