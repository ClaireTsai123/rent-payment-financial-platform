package com.claire.rentpaymentfinancialplatform.disbursement;

import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PropertyDisbursementResponse(
        UUID moneyMovementId,
        UUID paymentPlanId,
        MoneyMovementType type,
        MoneyMovementState state,
        BigDecimal amount,
        String currency,
        String operationKey,
        Instant createdAt
) {
}
