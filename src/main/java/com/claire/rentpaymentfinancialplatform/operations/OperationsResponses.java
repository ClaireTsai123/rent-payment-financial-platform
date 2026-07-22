package com.claire.rentpaymentfinancialplatform.operations;

import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType;
import com.claire.rentpaymentfinancialplatform.shared.domain.OutboxEventStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderTransactionStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderWebhookEventStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationExceptionType;
import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationRunStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.SettlementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class OperationsResponses {

    private OperationsResponses() {
    }

    public record MoneyMovementSummary(
            UUID id,
            UUID paymentPlanId,
            String renterId,
            MoneyMovementType type,
            MoneyMovementState state,
            BigDecimal amount,
            String currency,
            String operationKey,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record MoneyMovementDetail(
            UUID id,
            UUID paymentPlanId,
            String renterId,
            MoneyMovementType type,
            MoneyMovementState state,
            BigDecimal amount,
            String currency,
            String operationKey,
            long version,
            Instant createdAt,
            Instant updatedAt,
            List<MoneyMovementStateHistoryItem> stateHistory
    ) {
    }

    public record MoneyMovementStateHistoryItem(
            UUID id,
            MoneyMovementState fromState,
            MoneyMovementState toState,
            String reason,
            Instant changedAt
    ) {
    }

    public record ProviderTransactionResponse(
            UUID id,
            UUID moneyMovementId,
            UUID paymentAttemptId,
            String provider,
            String providerTransactionId,
            String providerIdempotencyKey,
            ProviderTransactionStatus normalizedStatus,
            String rawStatus,
            String settlementReference,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ProviderWebhookEventResponse(
            UUID id,
            String provider,
            String providerEventId,
            String providerTransactionId,
            ProviderTransactionStatus normalizedStatus,
            String rawPayload,
            ProviderWebhookEventStatus processingStatus,
            String failureReason,
            Instant occurredAt,
            Instant receivedAt,
            Instant processedAt
    ) {
    }

    public record OutboxEventResponse(
            UUID id,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payload,
            OutboxEventStatus status,
            int attempts,
            String lastError,
            Instant publishedAt,
            Instant nextAttemptAt,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record SettlementRecordResponse(
            UUID id,
            UUID moneyMovementId,
            UUID providerTransactionId,
            SettlementStatus status,
            BigDecimal expectedGrossAmount,
            BigDecimal expectedFeeAmount,
            BigDecimal expectedNetAmount,
            BigDecimal actualGrossAmount,
            BigDecimal actualFeeAmount,
            BigDecimal actualNetAmount,
            String currency,
            LocalDate expectedSettlementDate,
            LocalDate actualSettlementDate,
            String provider,
            String providerTransactionReference,
            String providerBatchReference,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ReconciliationRunResponse(
            UUID id,
            String sourceFile,
            ReconciliationRunStatus status,
            int totalRows,
            int matchedRows,
            int exceptionRows,
            Instant startedAt,
            Instant completedAt,
            String failureReason
    ) {
    }

    public record ReconciliationExceptionResponse(
            UUID id,
            UUID reconciliationRunId,
            ReconciliationExceptionType exceptionType,
            String provider,
            String providerTransactionReference,
            String message,
            String rawRecord,
            Instant createdAt
    ) {
    }
}
