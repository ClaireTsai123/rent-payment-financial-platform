package com.claire.rentpaymentfinancialplatform.renter;

import static com.claire.rentpaymentfinancialplatform.SecurityTestSupport.finopsToken;
import static com.claire.rentpaymentfinancialplatform.SecurityTestSupport.renterToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentPlanStatus;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovement;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementStateHistoryRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.PaymentAttemptRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransactionRepository;
import com.claire.rentpaymentfinancialplatform.webhook.ProviderWebhookEventRepository;
import java.math.BigDecimal;
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
class RenterPortalControllerTests extends PostgresIntegrationTest {

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
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private SettlementRecordRepository settlementRecordRepository;

    @Autowired
    private ProviderWebhookEventRepository webhookEventRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

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
    void listsOnlyAuthenticatedRentersPaymentPlansWithPagination() throws Exception {
        PaymentPlan renterPlan = paymentPlanRepository.saveAndFlush(newPaymentPlan("renter-a"));
        paymentPlanRepository.saveAndFlush(newPaymentPlan("renter-b"));

        mockMvc.perform(get("/api/v1/me/payment-plans")
                        .header("Authorization", renterToken("renter-a"))
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(renterPlan.getId().toString()))
                .andExpect(jsonPath("$.content[0].renterId").value("renter-a"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void paymentPlanListsDefaultToNewestFirstAndTwentyRows() throws Exception {
        PaymentPlan firstPlan = paymentPlanRepository.saveAndFlush(newPaymentPlan("renter-a"));
        Thread.sleep(5);
        PaymentPlan secondPlan = paymentPlanRepository.saveAndFlush(newPaymentPlan("renter-a"));

        mockMvc.perform(get("/api/v1/me/payment-plans")
                        .header("Authorization", renterToken("renter-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(secondPlan.getId().toString()))
                .andExpect(jsonPath("$.content[1].id").value(firstPlan.getId().toString()));
    }

    @Test
    void renterListPageSizeIsCapped() throws Exception {
        paymentPlanRepository.saveAndFlush(newPaymentPlan("renter-a"));

        mockMvc.perform(get("/api/v1/me/payment-plans")
                        .header("Authorization", renterToken("renter-a"))
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void returnsNotFoundForAnotherRentersPaymentPlan() throws Exception {
        PaymentPlan otherPlan = paymentPlanRepository.saveAndFlush(newPaymentPlan("renter-b"));

        mockMvc.perform(get("/api/v1/me/payment-plans/{paymentPlanId}", otherPlan.getId())
                        .header("Authorization", renterToken("renter-a")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_PLAN_NOT_FOUND"));
    }

    @Test
    void listsAndFiltersAuthenticatedRentersMoneyMovements() throws Exception {
        PaymentPlan renterPlan = paymentPlanRepository.saveAndFlush(newPaymentPlan("renter-a"));
        PaymentPlan otherPlan = paymentPlanRepository.saveAndFlush(newPaymentPlan("renter-b"));
        MoneyMovement renterMovement = moneyMovementRepository.saveAndFlush(newMoneyMovement(renterPlan, "movement-a"));
        moneyMovementRepository.saveAndFlush(newMoneyMovement(otherPlan, "movement-b"));

        mockMvc.perform(get("/api/v1/me/money-movements")
                        .header("Authorization", renterToken("renter-a"))
                        .param("paymentPlanId", renterPlan.getId().toString())
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(renterMovement.getId().toString()))
                .andExpect(jsonPath("$.content[0].paymentPlanId").value(renterPlan.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void returnsMoneyMovementDetailForOwningRenter() throws Exception {
        PaymentPlan paymentPlan = paymentPlanRepository.saveAndFlush(newPaymentPlan("renter-a"));
        MoneyMovement movement = moneyMovementRepository.saveAndFlush(newMoneyMovement(paymentPlan, "movement-detail"));

        mockMvc.perform(get("/api/v1/me/money-movements/{moneyMovementId}", movement.getId())
                        .header("Authorization", renterToken("renter-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(movement.getId().toString()))
                .andExpect(jsonPath("$.type").value("RENTER_COLLECTION"))
                .andExpect(jsonPath("$.state").value("PROCESSING"))
                .andExpect(jsonPath("$.operationKey").value("movement-detail"));
    }

    @Test
    void rejectsMissingAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/me/payment-plans"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsNonRenterRole() throws Exception {
        mockMvc.perform(get("/api/v1/me/payment-plans")
                        .header("Authorization", finopsToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void doesNotReturnAnotherRentersMoneyMovement() throws Exception {
        PaymentPlan otherPlan = paymentPlanRepository.saveAndFlush(newPaymentPlan("renter-b"));
        MoneyMovement otherMovement = moneyMovementRepository.saveAndFlush(newMoneyMovement(otherPlan, "movement-other"));

        mockMvc.perform(get("/api/v1/me/money-movements/{moneyMovementId}", otherMovement.getId())
                        .header("Authorization", renterToken("renter-a")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MONEY_MOVEMENT_NOT_FOUND"));
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

    private static MoneyMovement newMoneyMovement(PaymentPlan paymentPlan, String operationKey) {
        return new MoneyMovement(
                UUID.randomUUID(),
                paymentPlan,
                MoneyMovementType.RENTER_COLLECTION,
                MoneyMovementState.PROCESSING,
                new BigDecimal("500.00"),
                "USD",
                operationKey
        );
    }
}
