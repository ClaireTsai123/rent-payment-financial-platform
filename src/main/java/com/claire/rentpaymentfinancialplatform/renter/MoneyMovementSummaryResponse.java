package com.claire.rentpaymentfinancialplatform.renter;

import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MoneyMovementSummaryResponse(
        UUID id,
        UUID paymentPlanId,
        MoneyMovementType type,
        MoneyMovementState state,
        BigDecimal amount,
        String currency,
        String operationKey,
        Instant createdAt,
        Instant updatedAt
) {
}
