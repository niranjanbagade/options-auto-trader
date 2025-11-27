package com.trading.automated.nb.AutoTrader.services.brokers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.warrenstrange.googleauth.GoogleAuthenticator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class MotilalTradeService {

    private static final String LOGIN_URL = "https://openapi.motilaloswal.com/rest/login/v3/authdirectapi";    private static final String API_KEY = "gs12TbA691XIWgS1";
    private static final String CLIENT_ID = "NB9551";
    private static final String PASSWORD = "@1994India";
    private static final String MAC_ADDRESS = "00:11:22:33:44:55";
    private static final String CLIENT_LOCAL_IP = "192.168.1.1";
    private static final String CLIENT_PUBLIC_IP = "123.45.67.89"; // Placeholder for Public IP
    private static final String OS_NAME = "Windows 10";
    private static final String OS_VERSION = "10.0.19041";
    private static final String DEVICE_MODEL = "Desktop";
    private static final String MANUFACTURER = "Custom PC";
    private static final String BROWSER_NAME = "Chrome";
    private static final String BROWSER_VERSION = "105.0";

    public String getSymbol(String inputOptionString, String expiryDateString) {
        return null;
    }

    public String generateSession(String totp) throws Exception {
        // 1. Build the JSON Request Body
        String jsonBody = String.format("""
            {
              "ClientCode": "%s",
              "Password": "%s",
              "TOTP": "%s",
              "SourceID": "WEB"
            }
            """, CLIENT_ID, PASSWORD, totp);

        // 2. Create HttpClient
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // 3. Create HttpRequest
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_URL))
                .header("Content-Type", "application/json")
                .header("ApiKey", API_KEY)
                .header("User-Agent", "MOSL/V.1.1.0")
                .header("Accept", "application/json")
                .header("VendorInfo", "YOUR_APPLICATION_NAME")
                .header("SourceID", "WEB")
                .header("MacAddress", MAC_ADDRESS)
                .header("ClientLocalIP", CLIENT_LOCAL_IP)
                .header("ClientPublicIP", CLIENT_PUBLIC_IP)
                .header("osname", OS_NAME)
                .header("osversion", OS_VERSION)
                .header("devicemodel", DEVICE_MODEL)
                .header("manufacturer", MANUFACTURER)
                .header("browsername", BROWSER_NAME)
                .header("browserversion", BROWSER_VERSION)// <--- ADD THIS LINE
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // 4. Send the request
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 5. Process the response
        if (response.statusCode() == 200) {
            String responseBody = response.body();
            System.out.println("Login Success. Raw Response: " + responseBody); // Print raw response for debugging

            Gson gson = new Gson();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            // Expected token field name is 'AuthToken'
            if (jsonResponse.has("AuthToken") && !jsonResponse.get("AuthToken").isJsonNull()) {
                String authToken = jsonResponse.get("AuthToken").getAsString();
                System.out.println("Extracted Auth Token: " + authToken);
                return authToken;
            } else {
                // Handle cases where the status is SUCCESS but the token is missing,
                // or a business logic error is returned in the JSON body.
                String status = jsonResponse.has("status") ? jsonResponse.get("status").getAsString() : "UNKNOWN";
                String message = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "No message provided.";

                if ("FAILED".equals(status)) {
                    throw new Exception("Login FAILED (200 OK): " + message);
                }

                throw new Exception("Login succeeded (200 OK) but AuthToken field is missing in the response.");
            }
        } else {
            throw new Exception("Login failed with status code: " + response.statusCode() + " and response body: " + response.body());
        }
    }

    public static String generateTotpCode(String secretKey) {
        GoogleAuthenticator gAuth = new GoogleAuthenticator();

        // This method calculates the TOTP code for the current 30-second window.
        int totpCode = gAuth.getTotpPassword(secretKey);

        // Ensure the code is 6 digits long with leading zeros if necessary
        return String.format("%06d", totpCode);
    }

    public static void main(String[] args) {
        String totpCode = generateTotpCode("2YX2V2BPLEZRLWBGFKDTXOMFNE54JBV2");
        MotilalTradeService motilalTradeService = new MotilalTradeService();
        String sessionToken = null;
        try {
            sessionToken = motilalTradeService.generateSession(totpCode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Session Token: " + sessionToken);
    }
}
