package com.trading.automated.nb.AutoTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan
@EnableScheduling
@EnableRetry
@EnableAsync
public class AutoTraderApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoTraderApplication.class, args);
    }
}