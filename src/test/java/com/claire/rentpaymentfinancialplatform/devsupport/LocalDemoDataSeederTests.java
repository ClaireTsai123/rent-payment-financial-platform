package com.claire.rentpaymentfinancialplatform.devsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlan;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlanRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovement;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class LocalDemoDataSeederTests {

    @Mock
    private PaymentPlanRepository paymentPlanRepository;

    @Mock
    private MoneyMovementRepository moneyMovementRepository;

    @Test
    void seedsRenterDemoData() {
        when(paymentPlanRepository.findByBillingObligationId("billing-demo-august-2026")).thenReturn(Optional.empty());
        when(paymentPlanRepository.findByBillingObligationId("billing-demo-july-2026")).thenReturn(Optional.empty());
        when(paymentPlanRepository.save(any(PaymentPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(moneyMovementRepository.findByOperationKey("demo-renter-collection-august-2026")).thenReturn(Optional.empty());
        when(moneyMovementRepository.findByOperationKey("demo-renter-collection-july-2026")).thenReturn(Optional.empty());
        when(moneyMovementRepository.findByOperationKey("demo-property-disbursement-july-2026")).thenReturn(Optional.empty());
        when(moneyMovementRepository.save(any(MoneyMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new LocalDemoDataSeeder(paymentPlanRepository, moneyMovementRepository)
                .run(new DefaultApplicationArguments());

        ArgumentCaptor<PaymentPlan> paymentPlanCaptor = ArgumentCaptor.forClass(PaymentPlan.class);
        ArgumentCaptor<MoneyMovement> moneyMovementCaptor = ArgumentCaptor.forClass(MoneyMovement.class);
        verify(paymentPlanRepository, org.mockito.Mockito.times(2)).save(paymentPlanCaptor.capture());
        verify(moneyMovementRepository, org.mockito.Mockito.times(3)).save(moneyMovementCaptor.capture());

        assertThat(paymentPlanCaptor.getAllValues())
                .extracting(PaymentPlan::getRenterId)
                .containsOnly(LocalDemoDataSeeder.DEMO_RENTER_ID);
        assertThat(paymentPlanCaptor.getAllValues())
                .extracting(PaymentPlan::getBillingObligationId)
                .containsExactlyInAnyOrder("billing-demo-august-2026", "billing-demo-july-2026");
        assertThat(moneyMovementCaptor.getAllValues())
                .extracting(MoneyMovement::getOperationKey)
                .containsExactlyInAnyOrder(
                        "demo-renter-collection-august-2026",
                        "demo-renter-collection-july-2026",
                        "demo-property-disbursement-july-2026"
                );
    }

    @Test
    void seedDataIsSafeToRerunWithoutDuplicates() {
        PaymentPlan augustPlan = new PaymentPlan(
                java.util.UUID.randomUUID(),
                LocalDemoDataSeeder.DEMO_RENTER_ID,
                "billing-demo-august-2026",
                new java.math.BigDecimal("2425.00"),
                new java.math.BigDecimal("485.00"),
                new java.math.BigDecimal("1940.00"),
                java.time.LocalDate.of(2026, 8, 1),
                java.time.LocalDate.of(2026, 8, 15),
                com.claire.rentpaymentfinancialplatform.shared.domain.PaymentPlanStatus.ACTIVE
        );
        PaymentPlan julyPlan = new PaymentPlan(
                java.util.UUID.randomUUID(),
                LocalDemoDataSeeder.DEMO_RENTER_ID,
                "billing-demo-july-2026",
                new java.math.BigDecimal("2395.00"),
                new java.math.BigDecimal("479.00"),
                new java.math.BigDecimal("1916.00"),
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalDate.of(2026, 7, 15),
                com.claire.rentpaymentfinancialplatform.shared.domain.PaymentPlanStatus.COMPLETED
        );
        when(paymentPlanRepository.findByBillingObligationId("billing-demo-august-2026")).thenReturn(Optional.of(augustPlan));
        when(paymentPlanRepository.findByBillingObligationId("billing-demo-july-2026")).thenReturn(Optional.of(julyPlan));
        when(moneyMovementRepository.findByOperationKey("demo-renter-collection-august-2026"))
                .thenReturn(Optional.of(existingMovement(augustPlan, "demo-renter-collection-august-2026")));
        when(moneyMovementRepository.findByOperationKey("demo-renter-collection-july-2026"))
                .thenReturn(Optional.of(existingMovement(julyPlan, "demo-renter-collection-july-2026")));
        when(moneyMovementRepository.findByOperationKey("demo-property-disbursement-july-2026"))
                .thenReturn(Optional.of(existingMovement(julyPlan, "demo-property-disbursement-july-2026")));

        new LocalDemoDataSeeder(paymentPlanRepository, moneyMovementRepository)
                .run(new DefaultApplicationArguments());

        verify(paymentPlanRepository, never()).save(any(PaymentPlan.class));
        verify(moneyMovementRepository, never()).save(any(MoneyMovement.class));
    }

    private static MoneyMovement existingMovement(PaymentPlan paymentPlan, String operationKey) {
        return new MoneyMovement(
                java.util.UUID.randomUUID(),
                paymentPlan,
                com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType.RENTER_COLLECTION,
                com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState.PROCESSING,
                new java.math.BigDecimal("485.00"),
                "USD",
                operationKey
        );
    }
}
