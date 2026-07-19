package com.claire.rentpaymentfinancialplatform.provider;

import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType;
import java.math.BigDecimal;
import java.util.UUID;

public record ProviderPaymentRequest(
        UUID moneyMovementId,
        MoneyMovementType moneyMovementType,
        BigDecimal amount,
        String currency,
        String operationKey,
        String providerIdempotencyKey
) {
}
