package com.trading.automated.nb.AutoTrader.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.trading.automated.nb.AutoTrader.exceptions.ApiException;
import com.trading.automated.nb.AutoTrader.services.SignalProcessingService;

import java.time.Instant;

record SignalDto(
        String message,
        Long sentAt) {
}

@RestController
public class SignalsController {
    private static final Logger logger = LoggerFactory.getLogger(SignalsController.class);

    @Value("${secret.vercel.header}")
    private String vercelHeaderSecret;

    @Autowired
    private SignalProcessingService signalProcessingService;

    private final ExecutorService signalExecutor = Executors.newFixedThreadPool(5);

    @PostMapping("${telegram.bot.webhook-path}")
    public ResponseEntity<String> handleSignal(
            @RequestHeader(value = "X-Vercel-Token", required = true) String token,
            @RequestBody SignalDto signalDto) {

        if (!vercelHeaderSecret.equals(token)) {
            logger.warn("Invalid X-Vercel-Token received. Expected: [PROTECTED], Received: {}", token);
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        logger.info("Signal received: {}", signalDto);

        long ageInSeconds = Instant.now().getEpochSecond() - (signalDto.sentAt() / 1000);

        if (ageInSeconds > 20) {
            logger.warn("Signal is expired. Age: {} seconds", ageInSeconds);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Expired");
        }

        String message = signalDto.message();
        if (message != null && !message.isEmpty()) {
            signalExecutor.submit(() -> {
                try {
                    signalProcessingService.processSignal(message);
                } catch (ApiException e) {
                    logger.error("API Error during signal processing for message: {}", message, e);
                } catch (Exception e) {
                    logger.error("Unexpected error during signal processing for message: {}", message, e);
                }
            });
        }

        return ResponseEntity.ok("Signal received");
    }
}
