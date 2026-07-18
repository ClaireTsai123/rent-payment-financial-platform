package com.claire.rentpaymentfinancialplatform.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.claire.rentpaymentfinancialplatform.PostgresIntegrationTest;
import com.claire.rentpaymentfinancialplatform.collection.CreateRenterCollectionRequest;
import com.claire.rentpaymentfinancialplatform.shared.domain.IdempotencyStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class IdempotencyServiceTests extends PostgresIntegrationTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @BeforeEach
    void cleanDatabase() {
        idempotencyRecordRepository.deleteAll();
    }

    @Test
    void concurrentDuplicateStartCreatesOnlyOneIdempotencyRecord() throws Exception {
        String idempotencyKey = "idem-" + UUID.randomUUID();
        CreateRenterCollectionRequest request = new CreateRenterCollectionRequest(
                UUID.randomUUID(),
                "collection-" + UUID.randomUUID(),
                "USD"
        );
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<String> task = () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            try {
                idempotencyService.startOrReplay(idempotencyKey, IdempotencyOperation.RENTER_COLLECTION, request);
                return "STARTED";
            } catch (IdempotencyInProgressException exception) {
                return "IN_PROGRESS";
            }
        };

        var executorService = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> futures = new ArrayList<>();
            futures.add(executorService.submit(task));
            futures.add(executorService.submit(task));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(futures.get(0).get(5, TimeUnit.SECONDS), futures.get(1).get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("STARTED", "IN_PROGRESS");
        } finally {
            executorService.shutdownNow();
        }

        assertThat(idempotencyRecordRepository.findAll()).singleElement().satisfies(record -> {
            assertThat(record.getIdempotencyKey()).isEqualTo(idempotencyKey);
            assertThat(record.getOperation()).isEqualTo(IdempotencyOperation.RENTER_COLLECTION.name());
            assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.IN_PROGRESS);
        });
    }

    @Test
    void expiredExistingRecordIsRejectedBeforeReplayOrConflictEvaluation() {
        CreateRenterCollectionRequest request = new CreateRenterCollectionRequest(
                UUID.randomUUID(),
                "collection-" + UUID.randomUUID(),
                "USD"
        );
        String idempotencyKey = "idem-" + UUID.randomUUID();
        idempotencyRecordRepository.saveAndFlush(new IdempotencyRecord(
                UUID.randomUUID(),
                idempotencyKey,
                IdempotencyOperation.RENTER_COLLECTION.name(),
                idempotencyService.fingerprintForTesting(request),
                IdempotencyStatus.IN_PROGRESS,
                null,
                Instant.parse("2020-01-01T00:00:00Z")
        ));

        assertThatThrownBy(() -> idempotencyService.startOrReplay(
                idempotencyKey,
                IdempotencyOperation.RENTER_COLLECTION,
                request
        )).isInstanceOf(IdempotencyExpiredException.class);
    }

    @Test
    void fingerprintUsesNormalizedRequestValues() {
        UUID paymentPlanId = UUID.randomUUID();
        String operationKey = "collection-" + UUID.randomUUID();
        CreateRenterCollectionRequest original = new CreateRenterCollectionRequest(paymentPlanId, operationKey, "USD");
        CreateRenterCollectionRequest equivalent = new CreateRenterCollectionRequest(paymentPlanId, "  " + operationKey + "  ", " usd ");

        assertThat(idempotencyService.fingerprintForTesting(equivalent))
                .isEqualTo(idempotencyService.fingerprintForTesting(original));
    }
}
