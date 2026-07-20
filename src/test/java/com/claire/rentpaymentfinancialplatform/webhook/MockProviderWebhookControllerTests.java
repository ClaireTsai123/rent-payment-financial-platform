package com.claire.rentpaymentfinancialplatform.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claire.rentpaymentfinancialplatform.PostgresIntegrationTest;
import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyRecordRepository;
import com.claire.rentpaymentfinancialplatform.outbox.OutboxEventRepository;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlan;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlanRepository;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType;
import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentAttemptStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentPlanStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderTransactionStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderWebhookEventStatus;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementStateHistoryRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.PaymentAttemptRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransaction;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MockProviderWebhookControllerTests extends PostgresIntegrationTest {

    private static final String WEBHOOK_SECRET = "local-mock-webhook-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentPlanRepository paymentPlanRepository;

    @Autowired
    private MoneyMovementRepository moneyMovementRepository;

    @Autowired
    private ProviderTransactionRepository providerTransactionRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private MoneyMovementStateHistoryRepository stateHistoryRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProviderWebhookEventRepository webhookEventRepository;

    @BeforeEach
    void cleanDatabase() {
        webhookEventRepository.deleteAll();
        providerTransactionRepository.deleteAll();
        paymentAttemptRepository.deleteAll();
        stateHistoryRepository.deleteAll();
        outboxEventRepository.deleteAll();
        moneyMovementRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        paymentPlanRepository.deleteAll();
    }

    @ParameterizedTest
    @MethodSource("apiFlows")
    void appliesProcessingToSucceededWebhook(ApiFlow apiFlow) throws Exception {
        ProviderTransaction providerTransaction = createSubmittedMovement(apiFlow, "ok");

        sendWebhook(
                "event-" + UUID.randomUUID(),
                providerTransaction.getProviderTransactionId(),
                ProviderTransactionStatus.SUCCEEDED,
                WEBHOOK_SECRET
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("APPLIED"));

        assertThat(providerTransactionRepository.findAll()).singleElement()
                .satisfies(transaction -> assertThat(transaction.getNormalizedStatus()).isEqualTo(ProviderTransactionStatus.SUCCEEDED));
        assertThat(paymentAttemptRepository.findAll()).singleElement()
                .satisfies(attempt -> assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.SUCCEEDED));
        assertThat(moneyMovementRepository.findAll()).singleElement()
                .satisfies(moneyMovement -> assertThat(moneyMovement.getState()).isEqualTo(MoneyMovementState.SUCCEEDED));
        assertThat(stateHistoryRepository.findAll()).hasSize(2)
                .last()
                .satisfies(history -> {
                    assertThat(history.getFromState()).isEqualTo(MoneyMovementState.PROCESSING);
                    assertThat(history.getToState()).isEqualTo(MoneyMovementState.SUCCEEDED);
                    assertThat(history.getReason()).isEqualTo("WEBHOOK_SUCCEEDED");
                });
        assertThat(webhookEventRepository.findAll()).singleElement()
                .satisfies(event -> assertThat(event.getProcessingStatus()).isEqualTo(ProviderWebhookEventStatus.APPLIED));
    }

    @ParameterizedTest
    @MethodSource("apiFlows")
    void appliesProcessingToFailedWebhook(ApiFlow apiFlow) throws Exception {
        ProviderTransaction providerTransaction = createSubmittedMovement(apiFlow, "ok");

        sendWebhook(
                "event-" + UUID.randomUUID(),
                providerTransaction.getProviderTransactionId(),
                ProviderTransactionStatus.FAILED,
                WEBHOOK_SECRET
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("APPLIED"));

        assertThat(providerTransactionRepository.findAll()).singleElement()
                .satisfies(transaction -> assertThat(transaction.getNormalizedStatus()).isEqualTo(ProviderTransactionStatus.FAILED));
        assertThat(paymentAttemptRepository.findAll()).singleElement().satisfies(attempt -> {
            assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
            assertThat(attempt.getFailureCode()).isEqualTo("PROVIDER_WEBHOOK_FAILED");
        });
        assertThat(moneyMovementRepository.findAll()).singleElement()
                .satisfies(moneyMovement -> assertThat(moneyMovement.getState()).isEqualTo(MoneyMovementState.FAILED));
        assertThat(stateHistoryRepository.findAll()).hasSize(2)
                .last()
                .satisfies(history -> assertThat(history.getReason()).isEqualTo("WEBHOOK_FAILED"));
    }

    @ParameterizedTest
    @MethodSource("apiFlows")
    void resolvesAmbiguousTimeoutByWebhook(ApiFlow apiFlow) throws Exception {
        ProviderTransaction providerTransaction = createSubmittedMovement(apiFlow, "mock-timeout");
        assertThat(providerTransaction.getNormalizedStatus()).isEqualTo(ProviderTransactionStatus.UNKNOWN);
        assertThat(paymentAttemptRepository.findAll()).singleElement()
                .satisfies(attempt -> assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.AMBIGUOUS));

        sendWebhook(
                "event-" + UUID.randomUUID(),
                providerTransaction.getProviderTransactionId(),
                ProviderTransactionStatus.SUCCEEDED,
                WEBHOOK_SECRET
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("APPLIED"));

        assertThat(providerTransactionRepository.findAll()).singleElement()
                .satisfies(transaction -> assertThat(transaction.getNormalizedStatus()).isEqualTo(ProviderTransactionStatus.SUCCEEDED));
        assertThat(paymentAttemptRepository.findAll()).singleElement()
                .satisfies(attempt -> assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.SUCCEEDED));
        assertThat(moneyMovementRepository.findAll()).singleElement()
                .satisfies(moneyMovement -> assertThat(moneyMovement.getState()).isEqualTo(MoneyMovementState.SUCCEEDED));
    }

    @ParameterizedTest
    @MethodSource("apiFlows")
    void duplicateWebhookReplayReturnsSuccessWithoutSecondUpdate(ApiFlow apiFlow) throws Exception {
        ProviderTransaction providerTransaction = createSubmittedMovement(apiFlow, "ok");
        String providerEventId = "event-" + UUID.randomUUID();

        sendWebhook(providerEventId, providerTransaction.getProviderTransactionId(), ProviderTransactionStatus.SUCCEEDED, WEBHOOK_SECRET)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("APPLIED"));
        sendWebhook(providerEventId, providerTransaction.getProviderTransactionId(), ProviderTransactionStatus.SUCCEEDED, WEBHOOK_SECRET)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("DUPLICATE"));

        assertThat(webhookEventRepository.findAll()).hasSize(1);
        assertThat(stateHistoryRepository.findAll()).hasSize(2);
        assertThat(providerTransactionRepository.findAll()).singleElement()
                .satisfies(transaction -> assertThat(transaction.getNormalizedStatus()).isEqualTo(ProviderTransactionStatus.SUCCEEDED));
    }

    @ParameterizedTest
    @MethodSource("apiFlows")
    void rejectsInvalidSignature(ApiFlow apiFlow) throws Exception {
        ProviderTransaction providerTransaction = createSubmittedMovement(apiFlow, "ok");

        sendWebhook(
                "event-" + UUID.randomUUID(),
                providerTransaction.getProviderTransactionId(),
                ProviderTransactionStatus.SUCCEEDED,
                "wrong-secret"
        ).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("WEBHOOK_SIGNATURE_INVALID"));

        assertThat(webhookEventRepository.findAll()).isEmpty();
        assertThat(providerTransactionRepository.findAll()).singleElement()
                .satisfies(transaction -> assertThat(transaction.getNormalizedStatus()).isEqualTo(ProviderTransactionStatus.PROCESSING));
    }

    @ParameterizedTest
    @MethodSource("apiFlows")
    void persistsUnknownProviderTransactionForInvestigation(ApiFlow apiFlow) throws Exception {
        createSubmittedMovement(apiFlow, "ok");

        sendWebhook(
                "event-" + UUID.randomUUID(),
                "missing-provider-txn-" + UUID.randomUUID(),
                ProviderTransactionStatus.SUCCEEDED,
                WEBHOOK_SECRET
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("UNMATCHED"));

        assertThat(webhookEventRepository.findAll()).singleElement().satisfies(event -> {
            assertThat(event.getProcessingStatus()).isEqualTo(ProviderWebhookEventStatus.UNMATCHED);
            assertThat(event.getRawPayload()).contains("missing-provider-txn-");
            assertThat(event.getFailureReason()).isEqualTo("No provider transaction matched this webhook.");
        });
        assertThat(providerTransactionRepository.findAll()).singleElement()
                .satisfies(transaction -> assertThat(transaction.getNormalizedStatus()).isEqualTo(ProviderTransactionStatus.PROCESSING));
    }

    @ParameterizedTest
    @MethodSource("apiFlows")
    void staleWebhookDoesNotRegressTerminalState(ApiFlow apiFlow) throws Exception {
        ProviderTransaction providerTransaction = createSubmittedMovement(apiFlow, "ok");

        sendWebhook(
                "event-" + UUID.randomUUID(),
                providerTransaction.getProviderTransactionId(),
                ProviderTransactionStatus.SUCCEEDED,
                WEBHOOK_SECRET
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("APPLIED"));
        sendWebhook(
                "event-" + UUID.randomUUID(),
                providerTransaction.getProviderTransactionId(),
                ProviderTransactionStatus.FAILED,
                WEBHOOK_SECRET
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("IGNORED"));

        assertThat(providerTransactionRepository.findAll()).singleElement()
                .satisfies(transaction -> assertThat(transaction.getNormalizedStatus()).isEqualTo(ProviderTransactionStatus.SUCCEEDED));
        assertThat(paymentAttemptRepository.findAll()).singleElement()
                .satisfies(attempt -> assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.SUCCEEDED));
        assertThat(moneyMovementRepository.findAll()).singleElement()
                .satisfies(moneyMovement -> assertThat(moneyMovement.getState()).isEqualTo(MoneyMovementState.SUCCEEDED));
        assertThat(webhookEventRepository.findAll()).hasSize(2)
                .last()
                .satisfies(event -> assertThat(event.getProcessingStatus()).isEqualTo(ProviderWebhookEventStatus.IGNORED));
        assertThat(stateHistoryRepository.findAll()).hasSize(2);
    }

    private static Stream<ApiFlow> apiFlows() {
        return Stream.of(
                new ApiFlow("/api/v1/renter-collections", "collection", MoneyMovementType.RENTER_COLLECTION),
                new ApiFlow("/api/v1/property-disbursements", "disbursement", MoneyMovementType.PROPERTY_DISBURSEMENT)
        );
    }

    private ProviderTransaction createSubmittedMovement(ApiFlow apiFlow, String scenario) throws Exception {
        PaymentPlan paymentPlan = paymentPlanRepository.save(newPaymentPlan());
        String operationKey = apiFlow.operationPrefix() + "-" + scenario + "-" + UUID.randomUUID();

        mockMvc.perform(post(apiFlow.endpoint())
                        .header("Idempotency-Key", "idem-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentPlanId": "%s",
                                  "operationKey": "%s",
                                  "currency": "USD"
                                }
                                """.formatted(paymentPlan.getId(), operationKey)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value(apiFlow.expectedType().name()));

        return providerTransactionRepository.findAll().get(0);
    }

    private org.springframework.test.web.servlet.ResultActions sendWebhook(
            String providerEventId,
            String providerTransactionId,
            ProviderTransactionStatus providerStatus,
            String signature
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/provider-webhooks/mock-provider")
                .header("X-Mock-Provider-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "providerEventId": "%s",
                          "providerTransactionId": "%s",
                          "providerStatus": "%s",
                          "occurredAt": "%s"
                        }
                        """.formatted(providerEventId, providerTransactionId, providerStatus.name(), Instant.parse("2026-08-01T12:00:00Z"))));
    }

    private static PaymentPlan newPaymentPlan() {
        return new PaymentPlan(
                UUID.randomUUID(),
                "renter-" + UUID.randomUUID(),
                "billing-" + UUID.randomUUID(),
                new BigDecimal("2500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("2000.00"),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 15),
                PaymentPlanStatus.ACTIVE
        );
    }

    private record ApiFlow(String endpoint, String operationPrefix, MoneyMovementType expectedType) {
    }
}
