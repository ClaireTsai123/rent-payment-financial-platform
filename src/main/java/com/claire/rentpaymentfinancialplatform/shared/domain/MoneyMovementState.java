package com.claire.rentpaymentfinancialplatform.shared.domain;

public enum MoneyMovementState {
    CREATED,
    SUBMITTED,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    RETURNED,
    REVERSED
}
