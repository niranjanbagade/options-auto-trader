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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

@Service
public class WeeklyReportGenerationService {
    private static final Logger logger = LoggerFactory.getLogger(WeeklyReportGenerationService.class);
    private static final String APPLICATION_NAME = "Trading Consent App";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "src/main/resources/autotrader.json";
    private static final String SPREADSHEET_ID = "1TD_Q2eu3JOFrpJPF8WrsDuthORS9SzwrpjhUaqQ6OvI";
    private static final String RANGE = "ClientOnboardingForm!A2:J";

    public String generateWeeklyReport() {
        try {
            Sheets service = createSheetsService();
            ValueRange response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, RANGE)
                    .setValueRenderOption("FORMATTED_VALUE")
                    .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                logger.warn("No data found in Spreadsheet range: " + RANGE);
                return "";
            }

            // Map to store unique clients by email, keeping the latest entry (similar to
            // ActiveClientsService)
            // Using LinkedHashMap to maintain some order if needed, or just for
            // implementation stability
            Map<String, List<Object>> uniqueClients = new LinkedHashMap<>();

            for (List<Object> row : values) {
                try {
                    // Index 3 is Email Address (Client Email)
                    String emailAddress = getCellValue(row, 3);
                    if (emailAddress.isEmpty()) {
                        continue;
                    }

                    // Filter: Broker must be "groww"
                    String broker = getCellValue(row, 6);
                    if (!"groww".equalsIgnoreCase(broker)) {
                        continue;
                    }

                    // Filter: Remove client "VPrakash"
                    String clientName = getCellValue(row, 2);
                    if (clientName != null && clientName.trim().equalsIgnoreCase("VPrakash")) {
                        continue;
                    }

                    // Deduplication: Put in map (overwrites previous entry with same email)
                    uniqueClients.put(emailAddress, row);

                } catch (Exception e) {
                    logger.error("Error processing row: " + row, e);
                }
            }

            return convertToCsv(uniqueClients.values());

        } catch (IOException | GeneralSecurityException e) {
            logger.error("Failed to generate weekly report", e);
            throw new RuntimeException("Failed to generate weekly report", e);
        }
    }

    private String convertToCsv(Collection<List<Object>> rows) {
        StringBuilder csvBuilder = new StringBuilder();
        // Add Header - Excluding Broker (G), API Secret (H), Client Preference (I),
        // Lots (J)
        // A=0, B=1, C=2, D=3, E=4, F=5
        csvBuilder.append("Timestamp,Email Address,Client Name,Client Email,Client Phone Number,Telegram Channel ID\n");

        for (List<Object> row : rows) {
            List<String> validRow = new ArrayList<>();
            // Indices to include: 0, 1, 2, 3, 4, 5
            int[] indicesToInclude = { 0, 1, 2, 3, 4, 5 };

            for (int i : indicesToInclude) {
                String val = getCellValue(row, i);
                // Escape commas/quotes for CSV if necessary
                validRow.add(escapeCsv(val));
            }
            csvBuilder.append(String.join(",", validRow));
            csvBuilder.append("\n");
        }
        return csvBuilder.toString();
    }

    private String escapeCsv(String val) {
        if (val == null)
            return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            val = val.replace("\"", "\"\"");
            return "\"" + val + "\"";
        }
        return val;
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
        if (index >= row.size() || row.get(index) == null)
            return "";
        return row.get(index).toString().trim();
    }
}
