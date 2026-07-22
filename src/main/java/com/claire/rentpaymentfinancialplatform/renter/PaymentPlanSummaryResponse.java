package com.claire.rentpaymentfinancialplatform.renter;

import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentPlanStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentPlanSummaryResponse(
        UUID id,
        String renterId,
        String billingObligationId,
        BigDecimal rentAmount,
        BigDecimal initialCollectionAmount,
        BigDecimal repaymentAmount,
        LocalDate rentDueDate,
        LocalDate repaymentDueDate,
        PaymentPlanStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
