package com.claire.rentpaymentfinancialplatform.reconciliation;

import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationExceptionType;

record ProviderSettlementReconciliationItem(
        ProviderSettlementFileRow row,
        ReconciliationExceptionType exceptionType,
        String exceptionMessage
) {

    static ProviderSettlementReconciliationItem processable(ProviderSettlementFileRow row) {
        return new ProviderSettlementReconciliationItem(row, null, null);
    }

    static ProviderSettlementReconciliationItem exception(
            ProviderSettlementFileRow row,
            ReconciliationExceptionType exceptionType,
            String exceptionMessage
    ) {
        return new ProviderSettlementReconciliationItem(row, exceptionType, exceptionMessage);
    }

    boolean hasException() {
        return exceptionType != null;
    }
}
