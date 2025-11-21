package com.trading.automated.nb.AutoTrader.config.threads;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20); // Number of threads to keep alive
        executor.setMaxPoolSize(100); // Maximum number of threads
        executor.setQueueCapacity(500); // Number of tasks to queue before more threads are created
        executor.setThreadNamePrefix("AsyncOrder-");
        executor.initialize();
        return executor;
    }
}