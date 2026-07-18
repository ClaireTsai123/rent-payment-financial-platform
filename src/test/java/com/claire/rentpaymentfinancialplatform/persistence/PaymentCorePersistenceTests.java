package com.claire.rentpaymentfinancialplatform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.claire.rentpaymentfinancialplatform.PostgresIntegrationTest;
import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyRecord;
import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyRecordRepository;
import com.claire.rentpaymentfinancialplatform.outbox.OutboxEvent;
import com.claire.rentpaymentfinancialplatform.outbox.OutboxEventRepository;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlan;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlanRepository;
import com.claire.rentpaymentfinancialplatform.shared.domain.IdempotencyStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType;
import com.claire.rentpaymentfinancialplatform.shared.domain.OutboxEventStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentAttemptStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentPlanStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderTransactionStatus;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovement;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementStateHistory;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementStateHistoryRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.PaymentAttempt;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.PaymentAttemptRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransaction;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PaymentCorePersistenceTests extends PostgresIntegrationTest {

    @Autowired
    private PaymentPlanRepository paymentPlanRepository;

    @Autowired
    private MoneyMovementRepository moneyMovementRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private ProviderTransactionRepository providerTransactionRepository;

    @Autowired
    private MoneyMovementStateHistoryRepository stateHistoryRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void persistsPaymentCoreRecords() {
        PaymentPlan paymentPlan = paymentPlanRepository.saveAndFlush(newPaymentPlan("billing-" + UUID.randomUUID()));
        MoneyMovement moneyMovement = moneyMovementRepository.saveAndFlush(newMoneyMovement(paymentPlan, "collection-" + UUID.randomUUID()));
        PaymentAttempt attempt = paymentAttemptRepository.saveAndFlush(
                new PaymentAttempt(UUID.randomUUID(), moneyMovement, 1, PaymentAttemptStatus.SUBMITTED)
        );

        ProviderTransaction providerTransaction = providerTransactionRepository.saveAndFlush(new ProviderTransaction(
                UUID.randomUUID(),
                moneyMovement,
                attempt,
                "mock-provider",
                "provider-txn-" + UUID.randomUUID(),
                "provider-key-" + UUID.randomUUID(),
                ProviderTransactionStatus.PROCESSING,
                "processing"
        ));
        MoneyMovementStateHistory history = stateHistoryRepository.saveAndFlush(new MoneyMovementStateHistory(
                UUID.randomUUID(),
                moneyMovement,
                MoneyMovementState.CREATED,
                MoneyMovementState.SUBMITTED,
                "PROVIDER_SUBMITTED"
        ));
        IdempotencyRecord idempotencyRecord = idempotencyRecordRepository.saveAndFlush(new IdempotencyRecord(
                UUID.randomUUID(),
                "idem-" + UUID.randomUUID(),
                "RENTER_COLLECTION",
                "sha256:fingerprint",
                IdempotencyStatus.IN_PROGRESS,
                moneyMovement.getId(),
                Instant.parse("2026-08-01T00:00:00Z")
        ));
        OutboxEvent outboxEvent = outboxEventRepository.saveAndFlush(new OutboxEvent(
                UUID.randomUUID(),
                "MoneyMovement",
                moneyMovement.getId(),
                "money-movement.submitted",
                "{\"moneyMovementId\":\"" + moneyMovement.getId() + "\"}",
                OutboxEventStatus.PENDING,
                Instant.parse("2026-07-18T00:00:00Z")
        ));

        assertThat(providerTransactionRepository.findByProviderAndProviderTransactionId(
                providerTransaction.getProvider(),
                providerTransaction.getProviderTransactionId()
        )).map(ProviderTransaction::getId).contains(providerTransaction.getId());
        assertThat(moneyMovementRepository.findByOperationKey(moneyMovement.getOperationKey()))
                .map(MoneyMovement::getId)
                .contains(moneyMovement.getId());
        assertThat(idempotencyRecordRepository.findByIdempotencyKeyAndOperation(
                idempotencyRecord.getIdempotencyKey(),
                idempotencyRecord.getOperation()
        )).map(IdempotencyRecord::getId).contains(idempotencyRecord.getId());
        assertThat(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING))
                .extracting(OutboxEvent::getId)
                .contains(outboxEvent.getId());
        assertThat(outboxEventRepository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                OutboxEventStatus.PENDING,
                Instant.parse("2026-07-18T00:00:01Z")
        )).extracting(OutboxEvent::getId).contains(outboxEvent.getId());
        assertThat(idempotencyRecord.getExpiresAt()).isEqualTo(Instant.parse("2026-08-01T00:00:00Z"));
        assertThat(outboxEvent.getNextAttemptAt()).isEqualTo(Instant.parse("2026-07-18T00:00:00Z"));
        assertThat(history.getChangedAt()).isNotNull();
    }

    @Test
    void enforcesOperationKeyUniquenessForMoneyMovements() {
        PaymentPlan paymentPlan = paymentPlanRepository.saveAndFlush(newPaymentPlan("billing-" + UUID.randomUUID()));
        String operationKey = "collection-" + UUID.randomUUID();
        moneyMovementRepository.saveAndFlush(newMoneyMovement(paymentPlan, operationKey));

        assertThatThrownBy(() -> moneyMovementRepository.saveAndFlush(newMoneyMovement(paymentPlan, operationKey)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesIdempotencyKeyAndOperationUniqueness() {
        String idempotencyKey = "idem-" + UUID.randomUUID();
        idempotencyRecordRepository.saveAndFlush(new IdempotencyRecord(
                UUID.randomUUID(),
                idempotencyKey,
                "RENTER_COLLECTION",
                "sha256:first",
                IdempotencyStatus.IN_PROGRESS,
                null,
                Instant.parse("2026-08-01T00:00:00Z")
        ));

        assertThatThrownBy(() -> idempotencyRecordRepository.saveAndFlush(new IdempotencyRecord(
                UUID.randomUUID(),
                idempotencyKey,
                "RENTER_COLLECTION",
                "sha256:second",
                IdempotencyStatus.IN_PROGRESS,
                null,
                Instant.parse("2026-08-01T00:00:00Z")
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    private static PaymentPlan newPaymentPlan(String billingObligationId) {
        return new PaymentPlan(
                UUID.randomUUID(),
                "renter-" + UUID.randomUUID(),
                billingObligationId,
                new BigDecimal("2500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("2000.00"),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 15),
                PaymentPlanStatus.CREATED
        );
    }

    private static MoneyMovement newMoneyMovement(PaymentPlan paymentPlan, String operationKey) {
        return new MoneyMovement(
                UUID.randomUUID(),
                paymentPlan,
                MoneyMovementType.RENTER_COLLECTION,
                MoneyMovementState.CREATED,
                new BigDecimal("500.00"),
                "USD",
                operationKey
        );
    }
}
