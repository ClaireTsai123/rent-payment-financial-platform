package com.claire.rentpaymentfinancialplatform.paymentplan;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentPlanRepository extends JpaRepository<PaymentPlan, UUID> {

    Optional<PaymentPlan> findByBillingObligationId(String billingObligationId);
}
