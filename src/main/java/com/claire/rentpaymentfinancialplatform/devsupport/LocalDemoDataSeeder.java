package com.claire.rentpaymentfinancialplatform.devsupport;

import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlan;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlanRepository;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType;
import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentPlanStatus;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovement;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "dev"})
public class LocalDemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_RENTER_ID = "renter-123";

    private final PaymentPlanRepository paymentPlanRepository;
    private final MoneyMovementRepository moneyMovementRepository;

    public LocalDemoDataSeeder(
            PaymentPlanRepository paymentPlanRepository,
            MoneyMovementRepository moneyMovementRepository
    ) {
        this.paymentPlanRepository = paymentPlanRepository;
        this.moneyMovementRepository = moneyMovementRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (DemoPaymentPlan demoPlan : demoPlans()) {
            PaymentPlan paymentPlan = paymentPlanRepository.findByBillingObligationId(demoPlan.billingObligationId())
                    .orElseGet(() -> paymentPlanRepository.save(new PaymentPlan(
                            demoPlan.id(),
                            DEMO_RENTER_ID,
                            demoPlan.billingObligationId(),
                            demoPlan.rentAmount(),
                            demoPlan.initialCollectionAmount(),
                            demoPlan.repaymentAmount(),
                            demoPlan.rentDueDate(),
                            demoPlan.repaymentDueDate(),
                            demoPlan.status()
                    )));

            for (DemoMoneyMovement demoMovement : demoPlan.moneyMovements()) {
                moneyMovementRepository.findByOperationKey(demoMovement.operationKey())
                        .orElseGet(() -> moneyMovementRepository.save(new MoneyMovement(
                                demoMovement.id(),
                                paymentPlan,
                                demoMovement.type(),
                                demoMovement.state(),
                                demoMovement.amount(),
                                "USD",
                                demoMovement.operationKey()
                        )));
            }
        }
    }

    private static List<DemoPaymentPlan> demoPlans() {
        return List.of(
                new DemoPaymentPlan(
                        UUID.fromString("018f6f8d-1111-7000-8000-000000000101"),
                        "billing-demo-august-2026",
                        new BigDecimal("2425.00"),
                        new BigDecimal("485.00"),
                        new BigDecimal("1940.00"),
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 15),
                        PaymentPlanStatus.ACTIVE,
                        List.of(
                                new DemoMoneyMovement(
                                        UUID.fromString("018f6f8d-2222-7000-8000-000000000201"),
                                        MoneyMovementType.RENTER_COLLECTION,
                                        MoneyMovementState.PROCESSING,
                                        new BigDecimal("485.00"),
                                        "demo-renter-collection-august-2026"
                                )
                        )
                ),
                new DemoPaymentPlan(
                        UUID.fromString("018f6f8d-1111-7000-8000-000000000102"),
                        "billing-demo-july-2026",
                        new BigDecimal("2395.00"),
                        new BigDecimal("479.00"),
                        new BigDecimal("1916.00"),
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 15),
                        PaymentPlanStatus.COMPLETED,
                        List.of(
                                new DemoMoneyMovement(
                                        UUID.fromString("018f6f8d-2222-7000-8000-000000000202"),
                                        MoneyMovementType.RENTER_COLLECTION,
                                        MoneyMovementState.SUCCEEDED,
                                        new BigDecimal("479.00"),
                                        "demo-renter-collection-july-2026"
                                ),
                                new DemoMoneyMovement(
                                        UUID.fromString("018f6f8d-2222-7000-8000-000000000203"),
                                        MoneyMovementType.PROPERTY_DISBURSEMENT,
                                        MoneyMovementState.SUCCEEDED,
                                        new BigDecimal("2395.00"),
                                        "demo-property-disbursement-july-2026"
                                )
                        )
                )
        );
    }

    private record DemoPaymentPlan(
            UUID id,
            String billingObligationId,
            BigDecimal rentAmount,
            BigDecimal initialCollectionAmount,
            BigDecimal repaymentAmount,
            LocalDate rentDueDate,
            LocalDate repaymentDueDate,
            PaymentPlanStatus status,
            List<DemoMoneyMovement> moneyMovements
    ) {
    }

    private record DemoMoneyMovement(
            UUID id,
            MoneyMovementType type,
            MoneyMovementState state,
            BigDecimal amount,
            String operationKey
    ) {
    }
}
