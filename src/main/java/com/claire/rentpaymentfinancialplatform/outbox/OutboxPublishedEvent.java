package com.claire.rentpaymentfinancialplatform.outbox;

import java.util.UUID;

public record OutboxPublishedEvent(
        UUID outboxEventId,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String payload
) {
}
