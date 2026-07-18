package com.claire.rentpaymentfinancialplatform.shared.api;

import java.time.Instant;

public record ApiErrorResponse(
        String code,
        String message,
        Instant timestamp
) {
}
