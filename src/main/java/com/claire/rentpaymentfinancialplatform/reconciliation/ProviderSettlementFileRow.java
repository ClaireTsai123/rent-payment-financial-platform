package com.claire.rentpaymentfinancialplatform.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;

record ProviderSettlementFileRow(
        String provider,
        String providerTransactionReference,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal netAmount,
        String currency,
        LocalDate settlementDate,
        String providerBatchReference,
        String rawRecord
) {
}
