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
import com.trading.automated.nb.AutoTrader.config.ZerodhaTradingConfig;
import com.trading.automated.nb.AutoTrader.dtos.ConsentData;
import com.trading.automated.nb.AutoTrader.enums.MessageImportance;
import com.trading.automated.nb.AutoTrader.services.ZerodhaTradeService;
import com.trading.automated.nb.AutoTrader.telegram.TelegramOneToOneMessageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class GoogleSheetConsentService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetConsentService.class);
    private static final String APPLICATION_NAME = "Trading Consent App";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "src/main/resources/autotrader.json"; // Path to your downloaded JSON
    
    // Replace with your actual Spreadsheet ID (found in the URL of your Google Sheet)
    // Example: https://docs.google.com/spreadsheets/d/YOUR_SPREADSHEET_ID/edit
    private static final String SPREADSHEET_ID = "1TD_Q2eu3JOFrpJPF8WrsDuthORS9SzwrpjhUaqQ6OvI";
    
    // The range to read. Assumes data starts at A2 to E (skipping header)
    // Adjust columns based on your specific form output order
    private static final String RANGE = "Form Responses 3!A2:G1000";

    @Autowired
    private TelegramOneToOneMessageService telegramService;

    @Autowired
    private ZerodhaTradeService zerodhaTradeService;

    @Autowired
    private ZerodhaTradingConfig zerodhaTradingConfig;

    @PostConstruct
    public void init() {
        try {
            Map<String, ConsentData> consents = getTodayConsents();
            logger.info("Today's Consents: " + consents.size());
            logger.info("Geenrating accessToken for zerodha users");
            generateAccessTokens(consents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private void generateAccessTokens(Map<String, ConsentData> consents) {
        for (Map.Entry<String, ConsentData> entry : consents.entrySet()) {
            ConsentData consentData = entry.getValue();
            if(consentData.getBroker().equalsIgnoreCase("zerodha")){
                List<ZerodhaTradingConfig.ZerodhaAccount> accounts = zerodhaTradingConfig.getAccounts();
                ZerodhaTradingConfig.ZerodhaAccount account = accounts.stream()
                        .filter(acc -> acc.getEmail().equalsIgnoreCase(consentData.getEmail()))
                        .findFirst()
                        .orElse(null);
                if(account != null){
                    try {
                        String accessToken = zerodhaTradeService.generateSession(account.getKey(), account.getSecret(), consentData.getRequestToken());
                        account.setAccessToken(accessToken);
                        account.setLots(consentData.getLots());
                    } catch (IOException e) {
                        telegramService.sendMessage(account.getClientTelegramId(), account.getClientName()+" Failed to generate access token: " + e.getMessage(), MessageImportance.HIGH);
                    } catch (NoSuchAlgorithmException e) {
                        telegramService.sendMessage(account.getClientName(), "Failed to generate access token: " + e.getMessage(), MessageImportance.HIGH);
                    }
                }
            }
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
                String broker = getCellValue(row, 2);
                
                // Because of logic branching, columns might shift or be empty.
                // If Broker is Zerodha, Token is in col 3. If not, Token might be empty or N/A.
                String lots = getCellValue(row, 3);
                String requestToken = getCellValue(row, 4);
                String rating = getCellValue(row, 5);
                String consentStr = getCellValue(row, 6);

                LocalDateTime timestamp = parseTimestamp(timestampStr);

                // Filter: Only process if timestamp is from Today
                if (timestamp != null && timestamp.toLocalDate().equals(today)) {
                    
                    boolean isConsented = consentStr.toLowerCase().contains("Agree");

                    ConsentData data = new ConsentData(
                        email, 
                        broker, 
                        requestToken, 
                        isConsented,
                        timestamp,
                            Integer.parseInt(lots)
                    );

                    // Add to map (Key: Email)
                    // Note: If a user submitted multiple times today, this overwrites with the latest.
                    consentMap.put(email, data);
                }

            } catch (Exception e) {
                System.err.println("Error parsing row: " + row + " - " + e.getMessage());
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