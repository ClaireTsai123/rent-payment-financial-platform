package com.claire.rentpaymentfinancialplatform.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbox.publisher")
public record OutboxPublisherProperties(
        int batchSize,
        int maxAttempts,
        Duration retryDelay
) {

    public OutboxPublisherProperties {
        if (batchSize <= 0) {
            batchSize = 100;
        }
        if (maxAttempts <= 0) {
            maxAttempts = 3;
        }
        if (retryDelay == null) {
            retryDelay = Duration.ofMinutes(1);
        }
    }
}
