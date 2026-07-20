package com.claire.rentpaymentfinancialplatform.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claire.rentpaymentfinancialplatform.PostgresIntegrationTest;
import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyRecordRepository;
import com.claire.rentpaymentfinancialplatform.outbox.OutboxEventRepository;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlan;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlanRepository;
import com.claire.rentpaymentfinancialplatform.settlement.SettlementRecordRepository;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType;
import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentAttemptStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentPlanStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderTransactionStatus;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementStateHistoryRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.PaymentAttemptRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RenterCollectionControllerTests extends PostgresIntegrationTest {

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
    private SettlementRecordRepository settlementRecordRepository;

    @BeforeEach
    void cleanDatabase() {
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
    void createsRenterCollectionAgainstExistingPaymentPlan() throws Exception {
        PaymentPlan paymentPlan = paymentPlanRepository.save(newPaymentPlan());
        String operationKey = "collection-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/renter-collections")
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
                .andExpect(jsonPath("$.paymentPlanId").value(paymentPlan.getId().toString()))
                .andExpect(jsonPath("$.type").value("RENTER_COLLECTION"))
                .andExpect(jsonPath("$.state").value("PROCESSING"))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.operationKey").value(operationKey));

        assertThat(moneyMovementRepository.findAll()).singleElement().satisfies(moneyMovement -> {
            assertThat(moneyMovement.getPaymentPlan().getId()).isEqualTo(paymentPlan.getId());
            assertThat(moneyMovement.getType()).isEqualTo(MoneyMovementType.RENTER_COLLECTION);
            assertThat(moneyMovement.getState()).isEqualTo(MoneyMovementState.PROCESSING);
            assertThat(moneyMovement.getAmount()).isEqualByComparingTo(paymentPlan.getInitialCollectionAmount());
            assertThat(moneyMovement.getOperationKey()).isEqualTo(operationKey);
        });
        assertThat(paymentAttemptRepository.findAll()).singleElement().satisfies(attempt -> {
            assertThat(attempt.getAttemptNumber()).isEqualTo(1);
            assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PROCESSING);
            assertThat(attempt.getFailureCode()).isNull();
            assertThat(attempt.getFailureMessage()).isNull();
        });
        assertThat(providerTransactionRepository.findAll()).singleElement().satisfies(providerTransaction -> {
            assertThat(providerTransaction.getProvider()).isEqualTo("mock-provider");
            assertThat(providerTransaction.getProviderTransactionId()).startsWith("mock-txn-");
            assertThat(providerTransaction.getProviderIdempotencyKey()).isEqualTo(operationKey);
            assertThat(providerTransaction.getNormalizedStatus()).isEqualTo(ProviderTransactionStatus.PROCESSING);
            assertThat(providerTransaction.getRawStatus()).isEqualTo("PROCESSING");
        });
        assertThat(stateHistoryRepository.findAll()).singleElement().satisfies(history -> {
            assertThat(history.getFromState()).isEqualTo(MoneyMovementState.CREATED);
            assertThat(history.getToState()).isEqualTo(MoneyMovementState.PROCESSING);
            assertThat(history.getReason()).isEqualTo("PROVIDER_SUBMITTED");
        });
    }

    @Test
    void recordsDefinitiveProviderFailureForCollection() throws Exception {
        PaymentPlan paymentPlan = paymentPlanRepository.save(newPaymentPlan());
        String operationKey = "collection-mock-fail-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/renter-collections")
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
                .andExpect(jsonPath("$.state").value("FAILED"));

        assertThat(moneyMovementRepository.findAll()).singleElement()
                .satisfies(moneyMovement -> assertThat(moneyMovement.getState()).isEqualTo(MoneyMovementState.FAILED));
        assertThat(paymentAttemptRepository.findAll()).singleElement().satisfies(attempt -> {
            assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
            assertThat(attempt.getFailureCode()).isEqualTo("MOCK_PROVIDER_DECLINED");
            assertThat(attempt.getFailureMessage()).isEqualTo("Mock provider returned a definitive failure.");
        });
        assertThat(providerTransactionRepository.findAll()).singleElement().satisfies(providerTransaction -> {
            assertThat(providerTransaction.getProviderTransactionId()).startsWith("mock-failed-txn-");
            assertThat(providerTransaction.getProviderIdempotencyKey()).isEqualTo(operationKey);
            assertThat(providerTransaction.getNormalizedStatus()).isEqualTo(ProviderTransactionStatus.FAILED);
            assertThat(providerTransaction.getRawStatus()).isEqualTo("DECLINED");
        });
        assertThat(stateHistoryRepository.findAll()).singleElement().satisfies(history -> {
            assertThat(history.getFromState()).isEqualTo(MoneyMovementState.CREATED);
            assertThat(history.getToState()).isEqualTo(MoneyMovementState.FAILED);
            assertThat(history.getReason()).isEqualTo("PROVIDER_FAILED");
        });
    }

    @Test
    void recordsAmbiguousProviderTimeoutForCollectionWithoutFailingOrRetrying() throws Exception {
        PaymentPlan paymentPlan = paymentPlanRepository.save(newPaymentPlan());
        String operationKey = "collection-mock-timeout-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/renter-collections")
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
                .andExpect(jsonPath("$.state").value("PROCESSING"));

        assertThat(moneyMovementRepository.findAll()).singleElement()
                .satisfies(moneyMovement -> assertThat(moneyMovement.getState()).isEqualTo(MoneyMovementState.PROCESSING));
        assertThat(paymentAttemptRepository.findAll()).singleElement().satisfies(attempt -> {
            assertThat(attempt.getAttemptNumber()).isEqualTo(1);
            assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.AMBIGUOUS);
            assertThat(attempt.getFailureCode()).isEqualTo("MOCK_PROVIDER_TIMEOUT");
            assertThat(attempt.getFailureMessage()).isEqualTo("Mock provider timed out before final status was known.");
        });
        assertThat(providerTransactionRepository.findAll()).singleElement().satisfies(providerTransaction -> {
            assertThat(providerTransaction.getProviderTransactionId()).startsWith("mock-timeout-interaction-");
            assertThat(providerTransaction.getProviderIdempotencyKey()).isEqualTo(operationKey);
            assertThat(providerTransaction.getNormalizedStatus()).isEqualTo(ProviderTransactionStatus.UNKNOWN);
            assertThat(providerTransaction.getRawStatus()).isEqualTo("TIMEOUT");
        });
        assertThat(stateHistoryRepository.findAll()).singleElement().satisfies(history -> {
            assertThat(history.getFromState()).isEqualTo(MoneyMovementState.CREATED);
            assertThat(history.getToState()).isEqualTo(MoneyMovementState.PROCESSING);
            assertThat(history.getReason()).isEqualTo("PROVIDER_AMBIGUOUS");
        });
    }

    @Test
    void returnsNotFoundWhenPaymentPlanDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/v1/renter-collections")
                        .header("Idempotency-Key", "idem-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentPlanId": "%s",
                                  "operationKey": "collection-%s",
                                  "currency": "USD"
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_PLAN_NOT_FOUND"));

        assertThat(moneyMovementRepository.findAll()).isEmpty();
    }

    @Test
    void returnsConflictForDuplicateOperationKey() throws Exception {
        PaymentPlan paymentPlan = paymentPlanRepository.save(newPaymentPlan());
        String operationKey = "collection-" + UUID.randomUUID();
        String request = """
                {
                  "paymentPlanId": "%s",
                  "operationKey": "%s",
                  "currency": "USD"
                }
                """.formatted(paymentPlan.getId(), operationKey);

        mockMvc.perform(post("/api/v1/renter-collections")
                        .header("Idempotency-Key", "idem-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/renter-collections")
                        .header("Idempotency-Key", "idem-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_INTEGRITY_VIOLATION"));

        assertThat(moneyMovementRepository.findAll()).hasSize(1);
    }

    @Test
    void replaysCompletedCollectionForDuplicateIdempotencyKeyAndSameRequest() throws Exception {
        PaymentPlan paymentPlan = paymentPlanRepository.save(newPaymentPlan());
        String idempotencyKey = "idem-" + UUID.randomUUID();
        String request = """
                {
                  "paymentPlanId": "%s",
                  "operationKey": "collection-%s",
                  "currency": "USD"
                }
                """.formatted(paymentPlan.getId(), UUID.randomUUID());

        String firstResponse = mockMvc.perform(post("/api/v1/renter-collections")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String secondResponse = mockMvc.perform(post("/api/v1/renter-collections")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(secondResponse).isEqualTo(firstResponse);
        assertThat(moneyMovementRepository.findAll()).hasSize(1);
        assertThat(paymentAttemptRepository.findAll()).hasSize(1);
        assertThat(providerTransactionRepository.findAll()).hasSize(1);
        assertThat(stateHistoryRepository.findAll()).hasSize(1);
        assertThat(outboxEventRepository.findAll()).hasSize(1);
        assertThat(idempotencyRecordRepository.findAll()).singleElement().satisfies(record -> {
            assertThat(record.getResourceId()).isEqualTo(moneyMovementRepository.findAll().get(0).getId());
            assertThat(record.getResponsePayload()).isNotBlank();
            assertThat(record.getExpiresAt()).isAfter(record.getCreatedAt());
        });
    }

    @Test
    void rejectsCollectionWhenIdempotencyKeyIsReusedWithDifferentRequest() throws Exception {
        PaymentPlan paymentPlan = paymentPlanRepository.save(newPaymentPlan());
        String idempotencyKey = "idem-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/renter-collections")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentPlanId": "%s",
                                  "operationKey": "collection-%s",
                                  "currency": "USD"
                                }
                                """.formatted(paymentPlan.getId(), UUID.randomUUID())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/renter-collections")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentPlanId": "%s",
                                  "operationKey": "collection-%s",
                                  "currency": "USD"
                                }
                                """.formatted(paymentPlan.getId(), UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));

        assertThat(moneyMovementRepository.findAll()).hasSize(1);
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
}
