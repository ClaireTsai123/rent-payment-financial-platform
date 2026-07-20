package com.claire.rentpaymentfinancialplatform.shared.moneymovement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.claire.rentpaymentfinancialplatform.PostgresIntegrationTest;
import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyRecordRepository;
import com.claire.rentpaymentfinancialplatform.outbox.OutboxEventRepository;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlan;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlanRepository;
import com.claire.rentpaymentfinancialplatform.settlement.SettlementRecordRepository;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType;
import com.claire.rentpaymentfinancialplatform.shared.domain.OutboxEventStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentPlanStatus;
import com.claire.rentpaymentfinancialplatform.webhook.ProviderWebhookEventRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MoneyMovementStateTransitionServiceTests extends PostgresIntegrationTest {

    @Autowired
    private PaymentPlanRepository paymentPlanRepository;

    @Autowired
    private MoneyMovementRepository moneyMovementRepository;

    @Autowired
    private MoneyMovementStateHistoryRepository stateHistoryRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private ProviderTransactionRepository providerTransactionRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProviderWebhookEventRepository webhookEventRepository;

    @Autowired
    private SettlementRecordRepository settlementRecordRepository;

    @Autowired
    private MoneyMovementStateTransitionService transitionService;

    @BeforeEach
    void cleanDatabase() {
        webhookEventRepository.deleteAll();
        settlementRecordRepository.deleteAll();
        providerTransactionRepository.deleteAll();
        paymentAttemptRepository.deleteAll();
        stateHistoryRepository.deleteAll();
        outboxEventRepository.deleteAll();
        moneyMovementRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        paymentPlanRepository.deleteAll();
    }

    @Test
    void appliesValidTransitionAndCreatesHistory() {
        MoneyMovement moneyMovement = saveMoneyMovement(MoneyMovementState.CREATED);

        MoneyMovementStateTransitionResult result = transitionService.transition(
                moneyMovement,
                MoneyMovementState.PROCESSING,
                "PROVIDER_SUBMITTED"
        );

        assertThat(result.applied()).isTrue();
        assertThat(moneyMovementRepository.findById(moneyMovement.getId())).get()
                .satisfies(reloaded -> assertThat(reloaded.getState()).isEqualTo(MoneyMovementState.PROCESSING));
        assertThat(stateHistoryRepository.findAll()).singleElement().satisfies(history -> {
            assertThat(history.getFromState()).isEqualTo(MoneyMovementState.CREATED);
            assertThat(history.getToState()).isEqualTo(MoneyMovementState.PROCESSING);
            assertThat(history.getReason()).isEqualTo("PROVIDER_SUBMITTED");
        });
        assertThat(outboxEventRepository.findAll()).singleElement().satisfies(outboxEvent -> {
            assertThat(outboxEvent.getAggregateType()).isEqualTo("MoneyMovement");
            assertThat(outboxEvent.getAggregateId()).isEqualTo(moneyMovement.getId());
            assertThat(outboxEvent.getEventType()).isEqualTo("money-movement.state-changed");
            assertThat(outboxEvent.getPayload()).contains(
                    "\"moneyMovementId\":\"" + moneyMovement.getId() + "\"",
                    "\"fromState\":\"CREATED\"",
                    "\"toState\":\"PROCESSING\"",
                    "\"reason\":\"PROVIDER_SUBMITTED\""
            );
            assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(outboxEvent.getAttempts()).isZero();
            assertThat(outboxEvent.getNextAttemptAt()).isNotNull();
        });
    }

    @Test
    void rejectsInvalidRegressionFromSucceededToFailed() {
        MoneyMovement moneyMovement = saveMoneyMovement(MoneyMovementState.SUCCEEDED);

        MoneyMovementStateTransitionResult result = transitionService.transition(
                moneyMovement,
                MoneyMovementState.FAILED,
                "STALE_WEBHOOK"
        );

        assertThat(result.applied()).isFalse();
        assertThat(result.noOp()).isFalse();
        assertThat(result.rejectionReason()).contains("SUCCEEDED to FAILED");
        assertThat(moneyMovementRepository.findById(moneyMovement.getId())).get()
                .satisfies(reloaded -> assertThat(reloaded.getState()).isEqualTo(MoneyMovementState.SUCCEEDED));
        assertThat(stateHistoryRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void rejectsTransitionOutOfFailedTerminalState() {
        MoneyMovement moneyMovement = saveMoneyMovement(MoneyMovementState.FAILED);

        MoneyMovementStateTransitionResult result = transitionService.transition(
                moneyMovement,
                MoneyMovementState.PROCESSING,
                "STALE_WEBHOOK"
        );

        assertThat(result.applied()).isFalse();
        assertThat(result.rejectionReason()).contains("FAILED to PROCESSING");
        assertThat(stateHistoryRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void supportsPostSuccessReturnedTransition() {
        MoneyMovement moneyMovement = saveMoneyMovement(MoneyMovementState.SUCCEEDED);

        MoneyMovementStateTransitionResult result = transitionService.transition(
                moneyMovement,
                MoneyMovementState.RETURNED,
                "WEBHOOK_FAILED"
        );

        assertThat(result.applied()).isTrue();
        assertThat(moneyMovementRepository.findById(moneyMovement.getId())).get()
                .satisfies(reloaded -> assertThat(reloaded.getState()).isEqualTo(MoneyMovementState.RETURNED));
        assertThat(stateHistoryRepository.findAll()).singleElement()
                .satisfies(history -> assertThat(history.getFromState()).isEqualTo(MoneyMovementState.SUCCEEDED));
        assertThat(outboxEventRepository.findAll()).singleElement()
                .satisfies(outboxEvent -> assertThat(outboxEvent.getPayload()).contains("\"toState\":\"RETURNED\""));
    }

    @Test
    void supportsPostSuccessReversedTransition() {
        MoneyMovement moneyMovement = saveMoneyMovement(MoneyMovementState.SUCCEEDED);

        MoneyMovementStateTransitionResult result = transitionService.transition(
                moneyMovement,
                MoneyMovementState.REVERSED,
                "WEBHOOK_FAILED"
        );

        assertThat(result.applied()).isTrue();
        assertThat(moneyMovementRepository.findById(moneyMovement.getId())).get()
                .satisfies(reloaded -> assertThat(reloaded.getState()).isEqualTo(MoneyMovementState.REVERSED));
        assertThat(outboxEventRepository.findAll()).singleElement()
                .satisfies(outboxEvent -> assertThat(outboxEvent.getPayload()).contains("\"toState\":\"REVERSED\""));
    }

    @Test
    void treatsSameStateTransitionAsNoOpWithoutHistory() {
        MoneyMovement moneyMovement = saveMoneyMovement(MoneyMovementState.PROCESSING);

        MoneyMovementStateTransitionResult result = transitionService.transition(
                moneyMovement,
                MoneyMovementState.PROCESSING,
                "WEBHOOK_PROCESSING"
        );

        assertThat(result.applied()).isFalse();
        assertThat(result.noOp()).isTrue();
        assertThat(stateHistoryRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void rollsBackMoneyMovementAndOutboxWhenHistoryPersistenceFails() {
        MoneyMovement moneyMovement = saveMoneyMovement(MoneyMovementState.CREATED);

        assertThatThrownBy(() -> transitionService.transition(
                moneyMovement,
                MoneyMovementState.PROCESSING,
                "X".repeat(81)
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(moneyMovementRepository.findById(moneyMovement.getId())).get()
                .satisfies(reloaded -> assertThat(reloaded.getState()).isEqualTo(MoneyMovementState.CREATED));
        assertThat(stateHistoryRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    private MoneyMovement saveMoneyMovement(MoneyMovementState state) {
        PaymentPlan paymentPlan = paymentPlanRepository.saveAndFlush(new PaymentPlan(
                UUID.randomUUID(),
                "renter-" + UUID.randomUUID(),
                "billing-" + UUID.randomUUID(),
                new BigDecimal("2500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("2000.00"),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 15),
                PaymentPlanStatus.ACTIVE
        ));
        return moneyMovementRepository.saveAndFlush(new MoneyMovement(
                UUID.randomUUID(),
                paymentPlan,
                MoneyMovementType.RENTER_COLLECTION,
                state,
                new BigDecimal("500.00"),
                "USD",
                "movement-" + UUID.randomUUID()
        ));
    }
}
