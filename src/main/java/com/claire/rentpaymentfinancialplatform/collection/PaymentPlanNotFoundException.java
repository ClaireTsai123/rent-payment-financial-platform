package com.claire.rentpaymentfinancialplatform.collection;

import java.util.UUID;

public class PaymentPlanNotFoundException extends RuntimeException {

    public PaymentPlanNotFoundException(UUID paymentPlanId) {
        super("Payment plan not found: " + paymentPlanId);
    }
}
