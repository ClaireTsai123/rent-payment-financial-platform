package com.claire.rentpaymentfinancialplatform.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.claire.rentpaymentfinancialplatform.PostgresIntegrationTest;
import com.claire.rentpaymentfinancialplatform.shared.domain.OutboxEventStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "outbox.publisher.batch-size=10",
        "outbox.publisher.max-attempts=3",
        "outbox.publisher.retry-delay=PT0S",
        "outbox.publisher.fixed-delay=PT1H"
})
class OutboxPublisherTests extends PostgresIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private RecordingEventPublisher recordingEventPublisher;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        recordingEventPublisher.reset();
    }

    @Test
    void publishesEligiblePendingEventAndMarksItPublished() {
        OutboxEvent event = saveOutboxEvent("{\"message\":\"original-payload\"}", Instant.parse("2026-01-01T00:00:00Z"));

        int processed = outboxPublisher.publishPendingBatch();

        assertThat(processed).isEqualTo(1);
        assertThat(recordingEventPublisher.publishedEvents()).singleElement().satisfies(published -> {
            assertThat(published.outboxEventId()).isEqualTo(event.getId());
            assertThat(published.payload()).isEqualTo("{\"message\":\"original-payload\"}");
        });
        assertThat(outboxEventRepository.findById(event.getId())).get().satisfies(reloaded -> {
            assertThat(reloaded.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
            assertThat(reloaded.getPublishedAt()).isNotNull();
            assertThat(reloaded.getAttempts()).isZero();
            assertThat(reloaded.getLastError()).isNull();
        });
    }

    @Test
    void recordsFailureAndRetriesWhenEventLaterSucceeds() {
        OutboxEvent event = saveOutboxEvent("{\"scenario\":\"fail-once\"}", Instant.parse("2026-01-01T00:00:00Z"));
        recordingEventPublisher.failNext(event.getId());

        outboxPublisher.publishPendingBatch();

        assertThat(outboxEventRepository.findById(event.getId())).get().satisfies(reloaded -> {
            assertThat(reloaded.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(reloaded.getAttempts()).isEqualTo(1);
            assertThat(reloaded.getLastError()).contains("planned transient failure");
            assertThat(reloaded.getPublishedAt()).isNull();
        });

        outboxPublisher.publishPendingBatch();

        assertThat(recordingEventPublisher.publishedEvents()).hasSize(1);
        assertThat(outboxEventRepository.findById(event.getId())).get().satisfies(reloaded -> {
            assertThat(reloaded.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
            assertThat(reloaded.getAttempts()).isEqualTo(1);
            assertThat(reloaded.getPublishedAt()).isNotNull();
            assertThat(reloaded.getLastError()).isNull();
        });
    }

    @Test
    void marksEventFailedAfterMaximumAttempts() {
        OutboxEvent event = saveOutboxEvent("{\"scenario\":\"always-fail\"}", Instant.parse("2026-01-01T00:00:00Z"));
        recordingEventPublisher.alwaysFail(event.getId());

        outboxPublisher.publishPendingBatch();
        outboxPublisher.publishPendingBatch();
        outboxPublisher.publishPendingBatch();
        outboxPublisher.publishPendingBatch();

        assertThat(recordingEventPublisher.publishedEvents()).isEmpty();
        assertThat(outboxEventRepository.findById(event.getId())).get().satisfies(reloaded -> {
            assertThat(reloaded.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(reloaded.getAttempts()).isEqualTo(3);
            assertThat(reloaded.getLastError()).contains("planned permanent failure");
            assertThat(reloaded.getPublishedAt()).isNull();
        });
    }

    @Test
    void concurrentPollingDoesNotPublishTheSameEventTwice() throws Exception {
        List<UUID> eventIds = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            eventIds.add(saveOutboxEvent("{\"index\":" + index + "}", Instant.parse("2026-01-01T00:00:00Z")).getId());
        }
        recordingEventPublisher.pauseUntilBothPublishersAreReady();

        var executorService = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executorService.submit(() -> outboxPublisher.publishPendingBatch());
            Future<Integer> second = executorService.submit(() -> outboxPublisher.publishPendingBatch());

            assertThat(first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS)).isEqualTo(20);
        } finally {
            executorService.shutdownNow();
        }

        assertThat(recordingEventPublisher.publishedEvents()).hasSize(20);
        assertThat(recordingEventPublisher.publishedEvents())
                .extracting(OutboxPublishedEvent::outboxEventId)
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrderElementsOf(eventIds);
        assertThat(outboxEventRepository.findAll())
                .extracting(OutboxEvent::getStatus)
                .containsOnly(OutboxEventStatus.PUBLISHED);
    }

    private OutboxEvent saveOutboxEvent(String payload, Instant nextAttemptAt) {
        return outboxEventRepository.saveAndFlush(new OutboxEvent(
                UUID.randomUUID(),
                "MoneyMovement",
                UUID.randomUUID(),
                "money-movement.state-changed",
                payload,
                OutboxEventStatus.PENDING,
                nextAttemptAt
        ));
    }

    @TestConfiguration
    static class PublisherTestConfiguration {

        @Bean
        @Primary
        RecordingEventPublisher recordingEventPublisher() {
            return new RecordingEventPublisher();
        }
    }

    static class RecordingEventPublisher implements EventPublisher {

        private final List<OutboxPublishedEvent> publishedEvents = new CopyOnWriteArrayList<>();
        private final Set<UUID> failNext = ConcurrentHashMap.newKeySet();
        private final Set<UUID> alwaysFail = ConcurrentHashMap.newKeySet();
        private CountDownLatch readyLatch;
        private CountDownLatch releaseLatch;

        @Override
        public void publish(OutboxPublishedEvent event) {
            waitForConcurrentPublishersIfConfigured();
            if (alwaysFail.contains(event.outboxEventId())) {
                throw new IllegalStateException("planned permanent failure");
            }
            if (failNext.remove(event.outboxEventId())) {
                throw new IllegalStateException("planned transient failure");
            }
            publishedEvents.add(event);
        }

        List<OutboxPublishedEvent> publishedEvents() {
            return List.copyOf(publishedEvents);
        }

        void failNext(UUID eventId) {
            failNext.add(eventId);
        }

        void alwaysFail(UUID eventId) {
            alwaysFail.add(eventId);
        }

        void pauseUntilBothPublishersAreReady() {
            readyLatch = new CountDownLatch(2);
            releaseLatch = new CountDownLatch(1);
        }

        void reset() {
            publishedEvents.clear();
            failNext.clear();
            alwaysFail.clear();
            readyLatch = null;
            releaseLatch = null;
        }

        private void waitForConcurrentPublishersIfConfigured() {
            if (readyLatch == null || releaseLatch == null) {
                return;
            }
            readyLatch.countDown();
            try {
                if (readyLatch.await(5, TimeUnit.SECONDS)) {
                    releaseLatch.countDown();
                }
                releaseLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while coordinating concurrent publishers.", exception);
            }
        }
    }
}
