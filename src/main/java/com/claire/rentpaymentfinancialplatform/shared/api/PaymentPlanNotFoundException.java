package com.claire.rentpaymentfinancialplatform.shared.api;

import java.util.UUID;

public class PaymentPlanNotFoundException extends RuntimeException {

    public PaymentPlanNotFoundException(UUID paymentPlanId) {
        super("Payment plan not found: " + paymentPlanId);
    }
}
