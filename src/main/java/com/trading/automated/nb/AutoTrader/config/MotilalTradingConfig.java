package com.trading.automated.nb.AutoTrader.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Data
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "mo")
public class MotilalTradingConfig {
    
    private List<MotilalOswalAccount> accounts;

    public List<MotilalOswalAccount> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<MotilalOswalAccount> accounts) {
        this.accounts = accounts;
    }

    public static class MotilalOswalAccount {
        private String clientCode;
        private String clientTelegramId;
        private String clientName;
        private Boolean onlyBuy;
        private String apiKey;
        private String password;
        private String totpSecret; // The 32-char key from MO API Dashboard
        private String dob;        // Format: DD/MM/YYYY
        private int lots;

        public String getClientCode() {
            return clientCode;
        }

        public void setClientCode(String clientCode) {
            this.clientCode = clientCode;
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

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getTotpSecret() {
            return totpSecret;
        }

        public void setTotpSecret(String totpSecret) {
            this.totpSecret = totpSecret;
        }

        public String getDob() {
            return dob;
        }

        public void setDob(String dob) {
            this.dob = dob;
        }

        public int getLots() {
            return lots;
        }

        public void setLots(int lots) {
            this.lots = lots;
        }
    }
}