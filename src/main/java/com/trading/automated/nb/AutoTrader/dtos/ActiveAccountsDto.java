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
public class ActiveAccountsDto {
    private LocalDateTime timestamp;
    private String emailAddress;
    private String clientName;
    private String clientEmail;
    private String clientPhoneNumber;
    private String telegramChannelId;
    private String broker;
    private String apiSecret;
    private String clientPreference;
    private int lots;
    private String accessToken;
}