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
import com.trading.automated.nb.AutoTrader.dtos.ConsentData;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class DailyConsentService {
    private static final Logger logger = LoggerFactory.getLogger(DailyConsentService.class);
    private static final String APPLICATION_NAME = "Trading Consent App";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "src/main/resources/autotrader.json"; // Path to your downloaded JSON

    // Replace with your actual Spreadsheet ID (found in the URL of your Google Sheet)
    // Example: https://docs.google.com/spreadsheets/d/YOUR_SPREADSHEET_ID/edit
    private static final String SPREADSHEET_ID = "1TD_Q2eu3JOFrpJPF8WrsDuthORS9SzwrpjhUaqQ6OvI";

    // The range to read. Assumes data starts at A2 to E (skipping header)
    // Adjust columns based on your specific form output order
    private static final String RANGE = "DailyConsentForm!A2:G";

    @PostConstruct
    public void init() {
        try {
            Map<String, ConsentData> consents = getTodayConsents();
            logger.info("Today's Consents: {}", consents.size());
            logger.info("Generating accessToken for zerodha users");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, ConsentData> getTodayConsents() throws IOException, GeneralSecurityException {
        Sheets service = createSheetsService();

        ValueRange response = service.spreadsheets().values()
                .get(SPREADSHEET_ID, RANGE)
                .execute();

        List<List<Object>> values = response.getValues();
        Map<String, ConsentData> consentMap = new HashMap<>();

        if (values == null || values.isEmpty()) {
            return consentMap;
        }

        LocalDate today = LocalDate.now();

        // Iterate through rows
        for (List<Object> row : values) {
            try {
                // Parse row data
                // Column mapping depends on Form question order. 
                // Typically: Timestamp | Email | Broker | Request Token | Consent
                // Check your sheet columns to verify indices!

                String timestampStr = getCellValue(row, 0);
                String email = getCellValue(row, 1);
                // Because of logic branching, columns might shift or be empty.
                // If Broker is Zerodha, Token is in col 3. If not, Token might be empty or N/A.
                String lots = getCellValue(row, 2);
                String tokenOrTotpKey = getCellValue(row, 3);
                String consentStr = getCellValue(row, 5);
                boolean isConsented = "Agree".equalsIgnoreCase(consentStr);
                LocalDateTime timestamp = parseTimestamp(timestampStr);

                // Filter: Only process if timestamp is from Today
                if (timestamp != null && timestamp.toLocalDate().equals(today)) {
                    ConsentData data = new ConsentData(
                            email,
                            tokenOrTotpKey,
                            isConsented,
                            timestamp,
                            Integer.parseInt(lots)
                    );

                    // Add to map (Key: Email)
                    // Note: If a user submitted multiple times today, this overwrites with the latest.
                    consentMap.put(email, data);
                }

            } catch (Exception e) {
                logger.error("Error parsing row: {} - {}", row, e.getMessage());
            }
        }

        return consentMap;
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
        if (index >= row.size()) return "";
        return row.get(index).toString().trim();
    }

    private LocalDateTime parseTimestamp(String timestampStr) {
        // Google Forms typically uses "M/d/yyyy H:mm:ss" or "d/M/yyyy H:mm:ss" depending on locale settings of the Sheet
        // You might need to adjust this pattern based on your Sheet's format
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss");
            return LocalDateTime.parse(timestampStr, formatter);
        } catch (DateTimeParseException e) {
            // Fallback try for different format if needed
            return null;
        }
    }
}