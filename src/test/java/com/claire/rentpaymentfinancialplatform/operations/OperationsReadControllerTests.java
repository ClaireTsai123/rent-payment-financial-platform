package com.claire.rentpaymentfinancialplatform.operations;

import static com.claire.rentpaymentfinancialplatform.SecurityTestSupport.adminToken;
import static com.claire.rentpaymentfinancialplatform.SecurityTestSupport.finopsToken;
import static com.claire.rentpaymentfinancialplatform.SecurityTestSupport.renterToken;
import static com.claire.rentpaymentfinancialplatform.SecurityTestSupport.supportToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claire.rentpaymentfinancialplatform.PostgresIntegrationTest;
import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyRecordRepository;
import com.claire.rentpaymentfinancialplatform.outbox.OutboxEvent;
import com.claire.rentpaymentfinancialplatform.outbox.OutboxEventRepository;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlan;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlanRepository;
import com.claire.rentpaymentfinancialplatform.reconciliation.ReconciliationExceptionRecord;
import com.claire.rentpaymentfinancialplatform.reconciliation.ReconciliationExceptionRepository;
import com.claire.rentpaymentfinancialplatform.reconciliation.ReconciliationRun;
import com.claire.rentpaymentfinancialplatform.reconciliation.ReconciliationRunRepository;
import com.claire.rentpaymentfinancialplatform.settlement.SettlementRecord;
import com.claire.rentpaymentfinancialplatform.settlement.SettlementRecordRepository;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType;
import com.claire.rentpaymentfinancialplatform.shared.domain.OutboxEventStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentAttemptStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentPlanStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderTransactionStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationExceptionType;
import com.claire.rentpaymentfinancialplatform.shared.domain.SettlementStatus;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovement;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementStateHistory;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementStateHistoryRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.PaymentAttempt;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.PaymentAttemptRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransaction;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransactionRepository;
import com.claire.rentpaymentfinancialplatform.webhook.ProviderWebhookEvent;
import com.claire.rentpaymentfinancialplatform.webhook.ProviderWebhookEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OperationsReadControllerTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private ProviderWebhookEventRepository webhookEventRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private SettlementRecordRepository settlementRecordRepository;

    @Autowired
    private ReconciliationRunRepository reconciliationRunRepository;

    @Autowired
    private ReconciliationExceptionRepository reconciliationExceptionRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @BeforeEach
    void cleanDatabase() {
        reconciliationExceptionRepository.deleteAll();
        reconciliationRunRepository.deleteAll();
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
    void requiresSupportFinopsOrAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/ops/money-movements"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/ops/money-movements")
                        .header("Authorization", renterToken("renter-a")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/ops/money-movements")
                        .header("Authorization", supportToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/ops/money-movements")
                        .header("Authorization", finopsToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/ops/money-movements")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void listsMoneyMovementsWithPaginationSortingAndFilters() throws Exception {
        PaymentPlan plan = paymentPlanRepository.saveAndFlush(newPaymentPlan("renter-a"));
        MoneyMovement first = moneyMovementRepository.saveAndFlush(newMoneyMovement(
                plan,
                MoneyMovementType.RENTER_COLLECTION,
                MoneyMovementState.PROCESSING,
                "ops-first"
        ));
        Thread.sleep(5);
        MoneyMovement second = moneyMovementRepository.saveAndFlush(newMoneyMovement(
                plan,
                MoneyMovementType.PROPERTY_DISBURSEMENT,
                MoneyMovementState.SUCCEEDED,
                "ops-second"
        ));

        mockMvc.perform(get("/api/v1/ops/money-movements")
                        .header("Authorization", supportToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content[0].id").value(second.getId().toString()))
                .andExpect(jsonPath("$.content[1].id").value(first.getId().toString()));

        mockMvc.perform(get("/api/v1/ops/money-movements")
                        .header("Authorization", supportToken())
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));

        mockMvc.perform(get("/api/v1/ops/money-movements")
                        .header("Authorization", supportToken())
                        .param("state", "SUCCEEDED")
                        .param("type", "PROPERTY_DISBURSEMENT")
                        .param("paymentPlanId", plan.getId().toString())
                        .param("renterId", "renter-a")
                        .param("operationKey", "ops-second"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(second.getId().toString()));
    }

    @Test
    void returnsMoneyMovementDetailWithStateHistory() throws Exception {
        PaymentPlan plan = paymentPlanRepository.saveAndFlush(newPaymentPlan("renter-a"));
        MoneyMovement movement = moneyMovementRepository.saveAndFlush(newMoneyMovement(
                plan,
                MoneyMovementType.RENTER_COLLECTION,
                MoneyMovementState.SUCCEEDED,
                "ops-history"
        ));
        stateHistoryRepository.saveAndFlush(new MoneyMovementStateHistory(
                UUID.randomUUID(),
                movement,
                null,
                MoneyMovementState.PROCESSING,
                "PROVIDER_SUBMITTED"
        ));
        stateHistoryRepository.saveAndFlush(new MoneyMovementStateHistory(
                UUID.randomUUID(),
                movement,
                MoneyMovementState.PROCESSING,
                MoneyMovementState.SUCCEEDED,
                "WEBHOOK_SUCCEEDED"
        ));

        mockMvc.perform(get("/api/v1/ops/money-movements/{id}", movement.getId())
                        .header("Authorization", supportToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(movement.getId().toString()))
                .andExpect(jsonPath("$.stateHistory.length()").value(2))
                .andExpect(jsonPath("$.stateHistory[0].toState").value("PROCESSING"))
                .andExpect(jsonPath("$.stateHistory[1].reason").value("WEBHOOK_SUCCEEDED"));
    }

    @Test
    void listsAndLooksUpProviderTransactionsByExactReferences() throws Exception {
        ProviderTransaction transaction = seedProviderTransaction("mock-provider", "provider-txn-exact", ProviderTransactionStatus.PROCESSING);

        mockMvc.perform(get("/api/v1/ops/provider-transactions")
                        .header("Authorization", supportToken())
                        .param("provider", "mock-provider")
                        .param("status", "PROCESSING")
                        .param("providerTransactionId", "provider-txn-exact")
                        .param("providerIdempotencyKey", "provider-idem-provider-txn-exact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(transaction.getId().toString()));

        mockMvc.perform(get("/api/v1/ops/provider-transactions/{id}", transaction.getId())
                        .header("Authorization", supportToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerTransactionId").value("provider-txn-exact"));
    }

    @Test
    void listsWebhookOutboxSettlementAndReconciliationReadModelsWithFilters() throws Exception {
        ProviderTransaction transaction = seedProviderTransaction("mock-provider", "provider-txn-read-models", ProviderTransactionStatus.SUCCEEDED);
        ProviderWebhookEvent webhook = webhookEventRepository.saveAndFlush(new ProviderWebhookEvent(
                UUID.randomUUID(),
                "mock-provider",
                "provider-event-read-models",
                transaction.getProviderTransactionId(),
                ProviderTransactionStatus.SUCCEEDED,
                "{}",
                Instant.parse("2026-08-01T00:00:00Z")
        ));
        webhook.applied();
        webhookEventRepository.saveAndFlush(webhook);
        OutboxEvent outbox = outboxEventRepository.saveAndFlush(new OutboxEvent(
                UUID.randomUUID(),
                "MoneyMovement",
                transaction.getMoneyMovement().getId(),
                "money-movement.state-changed",
                "{\"state\":\"SUCCEEDED\"}",
                OutboxEventStatus.PENDING,
                Instant.parse("2026-08-01T00:00:00Z")
        ));
        SettlementRecord settlement = settlementRecordRepository.saveAndFlush(new SettlementRecord(
                UUID.randomUUID(),
                transaction.getMoneyMovement(),
                transaction,
                SettlementStatus.EXPECTED,
                new BigDecimal("500.00"),
                new BigDecimal("0.00"),
                new BigDecimal("500.00"),
                "USD",
                LocalDate.of(2026, 8, 2),
                "mock-provider",
                transaction.getProviderTransactionId()
        ));
        ReconciliationRun run = reconciliationRunRepository.saveAndFlush(new ReconciliationRun(
                UUID.randomUUID(),
                "ops-read-models.csv"
        ));
        ReconciliationExceptionRecord exception = reconciliationExceptionRepository.saveAndFlush(new ReconciliationExceptionRecord(
                UUID.randomUUID(),
                run,
                ReconciliationExceptionType.MISSING_SETTLEMENT,
                "mock-provider",
                "missing-provider-txn",
                "Missing expected settlement.",
                "raw-record"
        ));

        mockMvc.perform(get("/api/v1/ops/provider-webhook-events")
                        .header("Authorization", supportToken())
                        .param("provider", "mock-provider")
                        .param("status", "APPLIED")
                        .param("providerTransactionId", transaction.getProviderTransactionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(webhook.getId().toString()));

        mockMvc.perform(get("/api/v1/ops/outbox-events")
                        .header("Authorization", supportToken())
                        .param("aggregateType", "MoneyMovement")
                        .param("aggregateId", transaction.getMoneyMovement().getId().toString())
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(outbox.getId().toString()));

        mockMvc.perform(get("/api/v1/ops/settlement-records")
                        .header("Authorization", supportToken())
                        .param("provider", "mock-provider")
                        .param("status", "EXPECTED")
                        .param("providerTransactionReference", transaction.getProviderTransactionId())
                        .param("expectedSettlementDateFrom", "2026-08-01")
                        .param("expectedSettlementDateTo", "2026-08-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(settlement.getId().toString()));

        mockMvc.perform(get("/api/v1/ops/reconciliation-runs")
                        .header("Authorization", supportToken())
                        .param("sourceFile", "ops-read-models.csv")
                        .param("status", "STARTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(run.getId().toString()));

        mockMvc.perform(get("/api/v1/ops/reconciliation-exceptions")
                        .header("Authorization", supportToken())
                        .param("reconciliationRunId", run.getId().toString())
                        .param("exceptionType", "MISSING_SETTLEMENT")
                        .param("providerTransactionReference", "missing-provider-txn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(exception.getId().toString()));
    }

    @Test
    void returnsNotFoundAndInvalidFilterResponses() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/ops/money-movements/{id}", missingId)
                        .header("Authorization", supportToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("OPERATIONS_RESOURCE_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/ops/money-movements")
                        .header("Authorization", supportToken())
                        .param("state", "NOT_A_STATE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"));

        mockMvc.perform(get("/api/v1/ops/money-movements")
                        .header("Authorization", supportToken())
                        .param("createdFrom", "2026-08-02T00:00:00Z")
                        .param("createdTo", "2026-08-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILTER"));
    }

    private ProviderTransaction seedProviderTransaction(
            String provider,
            String providerTransactionId,
            ProviderTransactionStatus status
    ) {
        PaymentPlan plan = paymentPlanRepository.saveAndFlush(newPaymentPlan("renter-a"));
        MoneyMovement movement = moneyMovementRepository.saveAndFlush(newMoneyMovement(
                plan,
                MoneyMovementType.RENTER_COLLECTION,
                MoneyMovementState.PROCESSING,
                "movement-" + providerTransactionId
        ));
        PaymentAttempt attempt = paymentAttemptRepository.saveAndFlush(new PaymentAttempt(
                UUID.randomUUID(),
                movement,
                1,
                PaymentAttemptStatus.PROCESSING
        ));
        return providerTransactionRepository.saveAndFlush(new ProviderTransaction(
                UUID.randomUUID(),
                movement,
                attempt,
                provider,
                providerTransactionId,
                "provider-idem-" + providerTransactionId,
                status,
                status.name()
        ));
    }

    private static PaymentPlan newPaymentPlan(String renterId) {
        return new PaymentPlan(
                UUID.randomUUID(),
                renterId,
                "billing-" + UUID.randomUUID(),
                new BigDecimal("2500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("2000.00"),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 15),
                PaymentPlanStatus.ACTIVE
        );
    }

    private static MoneyMovement newMoneyMovement(
            PaymentPlan paymentPlan,
            MoneyMovementType type,
            MoneyMovementState state,
            String operationKey
    ) {
        return new MoneyMovement(
                UUID.randomUUID(),
                paymentPlan,
                type,
                state,
                new BigDecimal("500.00"),
                "USD",
                operationKey
        );
    }
}
