package com.trading.automated.nb.AutoTrader.controller;

import com.trading.automated.nb.AutoTrader.services.TradingSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    @Autowired
    private TradingSessionService tradingSessionService;

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
}
