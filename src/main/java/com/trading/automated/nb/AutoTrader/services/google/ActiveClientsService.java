package com.trading.automated.nb.AutoTrader.services.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.trading.automated.nb.AutoTrader.dtos.ActiveAccountsDto;
import com.trading.automated.nb.AutoTrader.services.brokers.ZerodhaTradeService;
import com.trading.automated.nb.AutoTrader.telegram.TelegramOneToOneMessageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActiveClientsService {
    private static final Logger logger = LoggerFactory.getLogger(ActiveClientsService.class);
    private static final String APPLICATION_NAME = "Trading Consent App";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "src/main/resources/autotrader.json";
    private static final String SPREADSHEET_ID = "1TD_Q2eu3JOFrpJPF8WrsDuthORS9SzwrpjhUaqQ6OvI";

    // UPDATED: Range extended to Column J to capture all 10 fields
    private static final String RANGE = "ClientOnboardingForm!A2:J";

    @Autowired
    private TelegramOneToOneMessageService telegramService;

    @Autowired
    private ZerodhaTradeService zerodhaTradeService;

    private Map<String, ActiveAccountsDto> accounts;

    public Map<String, ActiveAccountsDto> getAccounts() {
        return accounts;
    }

    @PostConstruct
    public void init() {
        try {
            accounts = getActiveClients();
            logger.info("Total Active clients loaded: " + accounts.size());
        } catch (IOException | GeneralSecurityException e) {
            logger.error("Failed to load active clients from Google Sheet", e);
            throw new RuntimeException(e);
        }
    }

    public Map<String, ActiveAccountsDto> getActiveClients() throws IOException, GeneralSecurityException {
        Sheets service = createSheetsService();

        // Ensure we get the formatted values (strings) exactly as seen in the sheet
        ValueRange response = service.spreadsheets().values()
                .get(SPREADSHEET_ID, RANGE)
                .setValueRenderOption("FORMATTED_VALUE")
                .execute();

        List<List<Object>> values = response.getValues();
        Map<String, ActiveAccountsDto> accountsMap = new HashMap<>();

        if (values == null || values.isEmpty()) {
            logger.warn("No data found in Spreadsheet range: " + RANGE);
            return accountsMap;
        }

        // Iterate through rows
        for (List<Object> row : values) {
            try {
                // 1. Safety Check: Ensure row has enough data (at least Email)
                // Index 1 is Email Address based on your data sample
                String emailAddress = getCellValue(row, 3);
                if (emailAddress.isEmpty()) {
                    continue; // Skip empty rows
                }

                // 2. Parse Timestamp
                String timestampStr = getCellValue(row, 0);
                LocalDateTime timestamp = parseTimestamp(timestampStr);

                if(!"groww".equalsIgnoreCase(getCellValue(row, 6))){
                    continue; // Skip non-Groww brokers
                }

                // 3. Map Fields
                ActiveAccountsDto dto = ActiveAccountsDto.builder()
                        .timestamp(timestamp)
                        .emailAddress(emailAddress)                           // Col B
                        .clientName(getCellValue(row, 2))                     // Col C
                        .clientEmail(getCellValue(row, 3))                    // Col D (Duplicate check?)
                        .clientPhoneNumber(getCellValue(row, 4))              // Col E
                        .telegramChannelId(getCellValue(row, 5))              // Col F
                        .broker(getCellValue(row, 6))                         // Col G
                        .apiSecret(getCellValue(row, 7))                      // Col H
                        .clientPreference(getCellValue(row, 8))               // Col I
                        .build();

                // 4. Put in Map (Key: Email Address)
                // Note: If the same email exists twice, this overwrites with the latest entry
                // (assuming the sheet adds new rows at the bottom).
                accountsMap.put(emailAddress, dto);

            } catch (Exception e) {
                logger.error("Error parsing row: " + row, e);
            }
        }

        return accountsMap;
    }

    private Sheets createSheetsService() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(CREDENTIALS_FILE_PATH))
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY));

        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private String getCellValue(List<Object> row, int index) {
        if (index >= row.size() || row.get(index) == null) return "";
        return row.get(index).toString().trim();
    }

    // Update this method in `src/main/java/com/trading/automated/nb/AutoTrader/services/google/ActiveClientsService.java`
    private LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isEmpty()) return null;

        // Accept common variants: single- or double-digit month/day/hour and also ISO if present
        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss"),   // e.g. 11/26/2025 9:07:58 or 1/2/2025 5:06:07
                DateTimeFormatter.ofPattern("MM/dd/yyyy H:mm:ss"), // e.g. 11/26/2025 09:07:58 (hour single-digit tolerated)
                DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss"),  // alternative
                DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        };

        for (DateTimeFormatter fmt : formatters) {
            try {
                return LocalDateTime.parse(timestampStr, fmt);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }

        // Final attempt: normalize common separators/spaces then try again
        String normalized = timestampStr.trim().replaceAll("\\s+", " ");
        for (DateTimeFormatter fmt : formatters) {
            try {
                return LocalDateTime.parse(normalized, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }

        logger.warn("Could not parse date: {}", timestampStr);
        return null;
    }

}