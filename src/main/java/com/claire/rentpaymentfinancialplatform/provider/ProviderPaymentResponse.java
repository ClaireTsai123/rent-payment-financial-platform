package com.claire.rentpaymentfinancialplatform.provider;

import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderTransactionStatus;

public record ProviderPaymentResponse(
        String provider,
        String providerTransactionId,
        String providerIdempotencyKey,
        ProviderTransactionStatus normalizedStatus,
        String rawStatus,
        String failureCode,
        String failureMessage
) {
}
