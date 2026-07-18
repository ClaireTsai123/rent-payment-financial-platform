package com.claire.rentpaymentfinancialplatform.disbursement;

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
import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentPlanStatus;
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
class PropertyDisbursementControllerTests extends PostgresIntegrationTest {

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

    @BeforeEach
    void cleanDatabase() {
        providerTransactionRepository.deleteAll();
        paymentAttemptRepository.deleteAll();
        stateHistoryRepository.deleteAll();
        outboxEventRepository.deleteAll();
        moneyMovementRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        paymentPlanRepository.deleteAll();
    }

    @Test
    void createsPropertyDisbursementAgainstExistingPaymentPlan() throws Exception {
        PaymentPlan paymentPlan = paymentPlanRepository.save(newPaymentPlan());
        String operationKey = "disbursement-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/property-disbursements")
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
                .andExpect(jsonPath("$.type").value("PROPERTY_DISBURSEMENT"))
                .andExpect(jsonPath("$.state").value("CREATED"))
                .andExpect(jsonPath("$.amount").value(2500.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.operationKey").value(operationKey));

        assertThat(moneyMovementRepository.findAll()).singleElement().satisfies(moneyMovement -> {
            assertThat(moneyMovement.getPaymentPlan().getId()).isEqualTo(paymentPlan.getId());
            assertThat(moneyMovement.getType()).isEqualTo(MoneyMovementType.PROPERTY_DISBURSEMENT);
            assertThat(moneyMovement.getState()).isEqualTo(MoneyMovementState.CREATED);
            assertThat(moneyMovement.getAmount()).isEqualByComparingTo(paymentPlan.getRentAmount());
            assertThat(moneyMovement.getOperationKey()).isEqualTo(operationKey);
        });
    }

    @Test
    void returnsNotFoundWhenPaymentPlanDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/v1/property-disbursements")
                        .header("Idempotency-Key", "idem-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentPlanId": "%s",
                                  "operationKey": "disbursement-%s",
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
        String operationKey = "disbursement-" + UUID.randomUUID();
        String request = """
                {
                  "paymentPlanId": "%s",
                  "operationKey": "%s",
                  "currency": "USD"
                }
                """.formatted(paymentPlan.getId(), operationKey);

        mockMvc.perform(post("/api/v1/property-disbursements")
                        .header("Idempotency-Key", "idem-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/property-disbursements")
                        .header("Idempotency-Key", "idem-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_INTEGRITY_VIOLATION"));

        assertThat(moneyMovementRepository.findAll()).hasSize(1);
    }

    @Test
    void replaysCompletedDisbursementForDuplicateIdempotencyKeyAndSameRequest() throws Exception {
        PaymentPlan paymentPlan = paymentPlanRepository.save(newPaymentPlan());
        String idempotencyKey = "idem-" + UUID.randomUUID();
        String request = """
                {
                  "paymentPlanId": "%s",
                  "operationKey": "disbursement-%s",
                  "currency": "USD"
                }
                """.formatted(paymentPlan.getId(), UUID.randomUUID());

        String firstResponse = mockMvc.perform(post("/api/v1/property-disbursements")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String secondResponse = mockMvc.perform(post("/api/v1/property-disbursements")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(secondResponse).isEqualTo(firstResponse);
        assertThat(moneyMovementRepository.findAll()).hasSize(1);
        assertThat(idempotencyRecordRepository.findAll()).singleElement().satisfies(record -> {
            assertThat(record.getResourceId()).isEqualTo(moneyMovementRepository.findAll().get(0).getId());
            assertThat(record.getResponsePayload()).isNotBlank();
            assertThat(record.getExpiresAt()).isAfter(record.getCreatedAt());
        });
    }

    @Test
    void rejectsDisbursementWhenIdempotencyKeyIsReusedWithDifferentRequest() throws Exception {
        PaymentPlan paymentPlan = paymentPlanRepository.save(newPaymentPlan());
        String idempotencyKey = "idem-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/property-disbursements")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentPlanId": "%s",
                                  "operationKey": "disbursement-%s",
                                  "currency": "USD"
                                }
                                """.formatted(paymentPlan.getId(), UUID.randomUUID())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/property-disbursements")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentPlanId": "%s",
                                  "operationKey": "disbursement-%s",
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
