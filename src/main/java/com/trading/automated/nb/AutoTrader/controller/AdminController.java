package com.trading.automated.nb.AutoTrader.controller;

import com.trading.automated.nb.AutoTrader.enums.MessageImportance;
import com.trading.automated.nb.AutoTrader.services.TradingSessionService;
import com.trading.automated.nb.AutoTrader.services.google.WeeklyReportGenerationService;
import com.trading.automated.nb.AutoTrader.telegram.TelegramOneToOneMessageService;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    @Autowired
    private TradingSessionService tradingSessionService;

    @Autowired
    private WeeklyReportGenerationService weeklyReportGenerationService;

    @Autowired
    private TelegramOneToOneMessageService telegramOneToOneMessageService;

    @GetMapping("/start")
    public ResponseEntity<String> startApplication() {
        try {
            tradingSessionService.startSession();
            return ResponseEntity.ok("Application started and data loaded successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to start application: " + e.getMessage());
        }
    }

    @GetMapping("/refresh")
    public ResponseEntity<String> refreshApplication() {
        try {
            tradingSessionService.refreshSession();
            return ResponseEntity.ok("Application refreshed successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to refresh application: " + e.getMessage());
        }
    }

    @GetMapping("/stop")
    public ResponseEntity<String> stopApplication() {
        try {
            tradingSessionService.stopSession();
            return ResponseEntity.ok("Application stopped and goodbye messages sent successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to stop application: " + e.getMessage());
        }
    }

    @GetMapping("/reminder")
    public ResponseEntity<String> sendReminder() {
        try {
            tradingSessionService.sendReminder();
            return ResponseEntity.ok("Reminder sent successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to send reminder: " + e.getMessage());
        }
    }

    @GetMapping("/weekly-report")
    public ResponseEntity<String> generateWeeklyReport() {
        try {
            List<String> weeklyReports = weeklyReportGenerationService.generateWeeklyReport();
            for (String report : weeklyReports) {
                telegramOneToOneMessageService.sendMessageOverloaded(
                        "1003576383206",
                        report,
                        MessageImportance.GOOD, "8540025997:AAGOV62e_1m_WZD-nRlD8vJw0s9-K2ZMZkE");
            }
            return ResponseEntity.ok(weeklyReports.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to generate weekly report: " + e.getMessage());
        }
    }
}
