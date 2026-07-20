package com.claire.rentpaymentfinancialplatform.outbox;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

@Component
public class MockEventPublisher implements EventPublisher {

    private final List<OutboxPublishedEvent> publishedEvents = new CopyOnWriteArrayList<>();

    @Override
    public void publish(OutboxPublishedEvent event) {
        publishedEvents.add(event);
    }

    public List<OutboxPublishedEvent> getPublishedEvents() {
        return List.copyOf(publishedEvents);
    }

    public void clear() {
        publishedEvents.clear();
    }
}
