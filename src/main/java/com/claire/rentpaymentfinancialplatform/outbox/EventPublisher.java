package com.claire.rentpaymentfinancialplatform.outbox;

public interface EventPublisher {

    void publish(OutboxPublishedEvent event);
}
