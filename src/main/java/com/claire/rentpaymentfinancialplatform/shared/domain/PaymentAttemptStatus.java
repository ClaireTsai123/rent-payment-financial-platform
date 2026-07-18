package com.claire.rentpaymentfinancialplatform.shared.domain;

public enum PaymentAttemptStatus {
    CREATED,
    SUBMITTED,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    AMBIGUOUS
}
