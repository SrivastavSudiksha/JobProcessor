package com.example.jobprocessor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.job")
public class AppProperties {

    private int maxRetries = 3;
    private long retryBaseDelayMs = 1000;
    private int workerPoolSize = 4;
    private long jobLockTtlMs = 30000;
    private long pollIntervalMs = 500;
}
