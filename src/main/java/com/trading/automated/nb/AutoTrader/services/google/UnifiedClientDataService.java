package com.trading.automated.nb.AutoTrader.services.google;

import com.trading.automated.nb.AutoTrader.dtos.ActiveAccountsDto;
import com.trading.automated.nb.AutoTrader.dtos.ConsentData;
import com.trading.automated.nb.AutoTrader.dtos.UnifiedClientData;
import com.trading.automated.nb.AutoTrader.enums.MessageImportance;
import com.trading.automated.nb.AutoTrader.services.brokers.GrowwTradeService;
import com.trading.automated.nb.AutoTrader.telegram.TelegramOneToOneMessageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

@Service
public class UnifiedClientDataService {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedClientDataService.class);

    @Autowired
    private ActiveClientsService activeClientsService;

    @Autowired
    private DailyConsentService googleSheetConsentService;

    @Autowired
    private TelegramOneToOneMessageService telegramService;

    @Autowired
    private GrowwTradeService growwTradeService;

    private Map<String, UnifiedClientData> unifiedClientDataCache = new HashMap<>();

    // @PostConstruct removed for manual trigger via AdminController
    public void init() {
        unifiedClientDataCache = getUnifiedClientDataCache();
        logger.info("Unified Client Data Service initialized with {} consented clients.",
                unifiedClientDataCache.size());
    }

    private void generateZerodhaAccessToken(ConsentData consentData, ActiveAccountsDto account) {
        // try {
        // String accessToken = zerodhaTradeService.generateSession(account.getApiKey(),
        // account.getApiSecret(), consentData.getTokenOrTotpKey());
        // account.setAccessToken(accessToken);
        // account.setLots(consentData.getLots());
        // telegramService.sendMessage(account.getTelegramChannelId(), "Session careted
        // for " + account.getClientName(), MessageImportance.GOOD);
        // } catch (IOException e) {
        // account.setAccessToken(null);
        // telegramService.sendMessage(account.getTelegramChannelId(),
        // account.getClientName() + " Failed to generate access token",
        // MessageImportance.HIGH);
        // } catch (NoSuchAlgorithmException e) {
        // account.setAccessToken(null);
        // telegramService.sendMessage(account.getTelegramChannelId(), "Failed to
        // generate access token", MessageImportance.HIGH);
        // }
    }

    private void generateGrowAccessToken(ConsentData consentData, ActiveAccountsDto account) {
        try {
            String accessToken = growwTradeService.generateSession(consentData.getTokenOrTotpKey(),
                    account.getApiSecret());
            account.setAccessToken(accessToken);
            account.setLots(consentData.getLots());
        } catch (Exception e) {
            logger.info("Failed to generate Groww access token for {}", account.getClientName());
            account.setAccessToken(null);
            telegramService.sendMessage(account.getTelegramChannelId(),
                    "Failed to generate access token. Kindly ensure hat you have filled the consent form correctly.",
                    MessageImportance.HIGH);
        }
    }

    public Map<String, UnifiedClientData> getMergedClientData() {
        return unifiedClientDataCache;
    }

    /**
     * Fetches data and returns ONLY clients who have explicitly given consent today
     * and exist in the active accounts master list.
     *
     * @return Map<String, UnifiedClientData> where Key is Email Address
     */
    public Map<String, UnifiedClientData> getUnifiedClientDataCache() {
        Map<String, UnifiedClientData> unifiedMap = new HashMap<>();

        try {
            // 1. Fetch Data from both sources
            Map<String, ActiveAccountsDto> activeAccountsMap = activeClientsService.getActiveClients();
            Map<String, ConsentData> dailyConsentsMap = googleSheetConsentService.getTodayConsents();

            logger.info("Merging data: Found {} active accounts and {} daily consents.",
                    activeAccountsMap.size(), dailyConsentsMap.size());

            // 2. Iterate over DAILY CONSENTS (Logic driven by who submitted the form)
            for (Map.Entry<String, ConsentData> entry : dailyConsentsMap.entrySet()) {
                String email = entry.getKey();
                ConsentData consent = entry.getValue();
                // FILTER 1: Must exist in Master Active Accounts (to get API keys/Secrets)
                ActiveAccountsDto account = activeAccountsMap.get(email);
                if (account != null) {
                    // FILTER 2: strict check for consentGiven == true
                    if (!consent.isConsentGiven()) {
                        logger.info("Skipping {}: Consent not given.", email);
                        telegramService.sendMessage(account.getTelegramChannelId(), "Consent not received.",
                                MessageImportance.MEDIUM);
                        continue;
                    }

                    switch (account.getBroker().toLowerCase()) {
                        case "zerodha":
                            // generateZerodhaAccessToken(consent, account);
                            // As the zerodha access token is already there in the consent from, we will use
                            // that.
                            // account.setAccessToken(consent.getTokenOrTotpKey());
                            break;
                        case "groww":
                            generateGrowAccessToken(consent, account);
                            break;
                        default:
                            continue;
                    }

                    if (null == account.getAccessToken()) {
                        logger.info("Skipping {}: Access Token generation failed.", email);
                        telegramService.sendMessage(account.getTelegramChannelId(), "Access Token generation failed.",
                                MessageImportance.HIGH);
                        continue;
                    }

                    if (consent.getLots() <= 0) {
                        logger.info("Skipping {}: Invalid lot size {}.", email, consent.getLots());
                        telegramService.sendMessage(account.getTelegramChannelId(),
                                "Invalid lot size specified minimum 2 lots should be traded", MessageImportance.HIGH);
                        continue;
                    }

                    // 3. Merge and Add to Result
                    UnifiedClientData unifiedData = mergeData(account, consent);
                    unifiedMap.put(email, unifiedData);
                    telegramService.sendMessage(unifiedData.getTelegramChannelId(),
                            "Consent received and access token generated. You are all set for today's trading with "
                                    + unifiedData.getLots() + " lots!",
                            MessageImportance.GOOD);
                } else {
                    // Log warning: User said "Yes" today but we don't have their API keys in the
                    // master sheet
                    logger.warn("Skipping consent for {}: User consented but not found in Active Accounts Master List.",
                            email);
                }
            }

        } catch (IOException | GeneralSecurityException e) {
            logger.error("Failed to fetch and merge client data", e);
            throw new RuntimeException("Error merging client data sources", e);
        }

        logger.info("Final merged list contains {} consented clients ready for trading.", unifiedMap.size());
        return unifiedMap;
    }

    /**
     * Helper method to combine the two DTOs.
     */
    private UnifiedClientData mergeData(ActiveAccountsDto account, ConsentData consent) {
        return UnifiedClientData.builder()
                // --- Static Data (From Active Accounts) ---
                .email(account.getEmailAddress())
                .clientName(account.getClientName())
                .clientPhoneNumber(account.getClientPhoneNumber())
                .telegramChannelId(account.getTelegramChannelId())
                .broker(account.getBroker())
                .apiSecret(account.getApiSecret())
                .clientPreference(account.getClientPreference())
                .accessToken(account.getAccessToken())
                // --- Dynamic Data (From Consent) ---
                // We can safely access consent fields directly as we filtered for null/false
                // previously
                .tokenOrTotpKey(consent.getTokenOrTotpKey())
                .consentGiven(consent.isConsentGiven())
                .lots(consent.getLots())
                .timestamp(consent.getTimestamp())
                .build();
    }
}