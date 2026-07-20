package com.claire.rentpaymentfinancialplatform.outbox;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final EventPublisher eventPublisher;
    private final OutboxPublisherProperties properties;
    private final Clock clock;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            EventPublisher eventPublisher,
            OutboxPublisherProperties properties
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public int publishPendingBatch() {
        Instant now = Instant.now(clock);
        var events = outboxEventRepository.findEligibleForPublishing(now, properties.batchSize());
        events.forEach(event -> publishOne(event, now));
        return events.size();
    }

    private void publishOne(OutboxEvent event, Instant now) {
        try {
            eventPublisher.publish(new OutboxPublishedEvent(
                    event.getId(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getEventType(),
                    event.getPayload()
            ));
            event.markPublished(now);
        } catch (RuntimeException exception) {
            event.recordPublishFailure(
                    truncate(exception.getMessage()),
                    now.plus(properties.retryDelay()),
                    properties.maxAttempts()
            );
        }
        outboxEventRepository.saveAndFlush(event);
    }

    private static String truncate(String message) {
        if (message == null) {
            return "Publisher failed without an error message.";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
