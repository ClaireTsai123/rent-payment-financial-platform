package com.claire.rentpaymentfinancialplatform.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "outbox.publisher", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledOutboxPublisher {

    private final OutboxPublisher outboxPublisher;

    public ScheduledOutboxPublisher(OutboxPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay:PT5S}")
    public void publishPendingOutboxEvents() {
        outboxPublisher.publishPendingBatch();
    }
}
