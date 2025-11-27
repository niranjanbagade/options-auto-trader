package com.trading.automated.nb.AutoTrader.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedClientData {
    
    // --- Identity & Contact ---
    private String email;              // Merged from 'emailAddress' (ActiveAccounts) and 'email' (ConsentData)
    private String clientName;         // From ActiveAccountsDto
    private String clientEmail;        // From ActiveAccountsDto (kept separate in case it differs from primary email)
    private String clientPhoneNumber;  // From ActiveAccountsDto
    private String telegramChannelId;  // From ActiveAccountsDto

    // --- Trading Configuration ---
    private String broker;             // Common to both
    private String apiKey;             // From ActiveAccountsDto
    private String apiSecret;          // From ActiveAccountsDto
    private String tokenOrTotpKey;       // From ConsentData (Specific to Zerodha usually)
    private String accessToken;        // From ActiveAccountsDto
    private String clientPreference;   // From ActiveAccountsDto
    
    // --- Consent & State ---
    private boolean consentGiven;      // From ConsentData
    private int lots;                  // From ConsentData
    private LocalDateTime timestamp;   // Common to both (Timestamp of entry or consent)
}