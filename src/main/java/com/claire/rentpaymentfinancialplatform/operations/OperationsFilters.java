package com.claire.rentpaymentfinancialplatform.operations;

import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType;
import com.claire.rentpaymentfinancialplatform.shared.domain.OutboxEventStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderTransactionStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderWebhookEventStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationExceptionType;
import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationRunStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.SettlementStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

record MoneyMovementOperationsFilter(
        UUID id,
        UUID paymentPlanId,
        String renterId,
        MoneyMovementState state,
        MoneyMovementType type,
        String operationKey,
        Instant createdFrom,
        Instant createdTo
) {
}

record ProviderTransactionOperationsFilter(
        UUID id,
        UUID moneyMovementId,
        UUID paymentAttemptId,
        String provider,
        ProviderTransactionStatus status,
        String providerTransactionId,
        String providerIdempotencyKey,
        Instant createdFrom,
        Instant createdTo
) {
}

record ProviderWebhookEventOperationsFilter(
        UUID id,
        String provider,
        String providerEventId,
        String providerTransactionId,
        ProviderTransactionStatus normalizedStatus,
        ProviderWebhookEventStatus status,
        Instant occurredFrom,
        Instant occurredTo,
        Instant receivedFrom,
        Instant receivedTo
) {
}

record OutboxEventOperationsFilter(
        UUID id,
        UUID aggregateId,
        String aggregateType,
        String eventType,
        OutboxEventStatus status,
        Instant createdFrom,
        Instant createdTo
) {
}

record SettlementRecordOperationsFilter(
        UUID id,
        UUID moneyMovementId,
        UUID providerTransactionId,
        String provider,
        SettlementStatus status,
        String providerTransactionReference,
        String providerBatchReference,
        LocalDate expectedSettlementDateFrom,
        LocalDate expectedSettlementDateTo,
        LocalDate actualSettlementDateFrom,
        LocalDate actualSettlementDateTo,
        Instant createdFrom,
        Instant createdTo
) {
}

record ReconciliationRunOperationsFilter(
        UUID id,
        ReconciliationRunStatus status,
        String sourceFile,
        Instant startedFrom,
        Instant startedTo,
        Instant completedFrom,
        Instant completedTo
) {
}

record ReconciliationExceptionOperationsFilter(
        UUID id,
        UUID reconciliationRunId,
        ReconciliationExceptionType exceptionType,
        String provider,
        String providerTransactionReference,
        Instant createdFrom,
        Instant createdTo
) {
}
