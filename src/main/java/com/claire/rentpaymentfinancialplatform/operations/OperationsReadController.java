package com.claire.rentpaymentfinancialplatform.operations;

import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.MoneyMovementDetail;
import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.MoneyMovementSummary;
import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.OutboxEventResponse;
import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.ProviderTransactionResponse;
import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.ProviderWebhookEventResponse;
import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.ReconciliationExceptionResponse;
import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.ReconciliationRunResponse;
import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.SettlementRecordResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops")
@PreAuthorize("hasAnyRole('SUPPORT', 'FINOPS', 'ADMIN')")
public class OperationsReadController {

    private final OperationsQueryService operationsQueryService;

    public OperationsReadController(OperationsQueryService operationsQueryService) {
        this.operationsQueryService = operationsQueryService;
    }

    @GetMapping("/money-movements")
    Page<MoneyMovementSummary> listMoneyMovements(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) UUID paymentPlanId,
            @RequestParam(required = false) String renterId,
            @RequestParam(required = false) MoneyMovementState state,
            @RequestParam(required = false) MoneyMovementType type,
            @RequestParam(required = false) String operationKey,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return operationsQueryService.listMoneyMovements(
                new MoneyMovementOperationsFilter(id, paymentPlanId, renterId, state, type, operationKey, createdFrom, createdTo),
                pageable
        );
    }

    @GetMapping("/money-movements/{id}")
    MoneyMovementDetail getMoneyMovement(@PathVariable UUID id) {
        return operationsQueryService.getMoneyMovement(id);
    }

    @GetMapping("/provider-transactions")
    Page<ProviderTransactionResponse> listProviderTransactions(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) UUID moneyMovementId,
            @RequestParam(required = false) UUID paymentAttemptId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) ProviderTransactionStatus status,
            @RequestParam(required = false) String providerTransactionId,
            @RequestParam(required = false) String providerIdempotencyKey,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return operationsQueryService.listProviderTransactions(
                new ProviderTransactionOperationsFilter(
                        id,
                        moneyMovementId,
                        paymentAttemptId,
                        provider,
                        status,
                        providerTransactionId,
                        providerIdempotencyKey,
                        createdFrom,
                        createdTo
                ),
                pageable
        );
    }

    @GetMapping("/provider-transactions/{id}")
    ProviderTransactionResponse getProviderTransaction(@PathVariable UUID id) {
        return operationsQueryService.getProviderTransaction(id);
    }

    @GetMapping("/provider-webhook-events")
    Page<ProviderWebhookEventResponse> listProviderWebhookEvents(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String providerEventId,
            @RequestParam(required = false) String providerTransactionId,
            @RequestParam(required = false) ProviderTransactionStatus normalizedStatus,
            @RequestParam(required = false) ProviderWebhookEventStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant receivedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant receivedTo,
            @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return operationsQueryService.listProviderWebhookEvents(
                new ProviderWebhookEventOperationsFilter(
                        id,
                        provider,
                        providerEventId,
                        providerTransactionId,
                        normalizedStatus,
                        status,
                        occurredFrom,
                        occurredTo,
                        receivedFrom,
                        receivedTo
                ),
                pageable
        );
    }

    @GetMapping("/provider-webhook-events/{id}")
    ProviderWebhookEventResponse getProviderWebhookEvent(@PathVariable UUID id) {
        return operationsQueryService.getProviderWebhookEvent(id);
    }

    @GetMapping("/outbox-events")
    Page<OutboxEventResponse> listOutboxEvents(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) UUID aggregateId,
            @RequestParam(required = false) String aggregateType,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) OutboxEventStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return operationsQueryService.listOutboxEvents(
                new OutboxEventOperationsFilter(id, aggregateId, aggregateType, eventType, status, createdFrom, createdTo),
                pageable
        );
    }

    @GetMapping("/outbox-events/{id}")
    OutboxEventResponse getOutboxEvent(@PathVariable UUID id) {
        return operationsQueryService.getOutboxEvent(id);
    }

    @GetMapping("/settlement-records")
    Page<SettlementRecordResponse> listSettlementRecords(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) UUID moneyMovementId,
            @RequestParam(required = false) UUID providerTransactionId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(required = false) String providerTransactionReference,
            @RequestParam(required = false) String providerBatchReference,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedSettlementDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedSettlementDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate actualSettlementDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate actualSettlementDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return operationsQueryService.listSettlementRecords(
                new SettlementRecordOperationsFilter(
                        id,
                        moneyMovementId,
                        providerTransactionId,
                        provider,
                        status,
                        providerTransactionReference,
                        providerBatchReference,
                        expectedSettlementDateFrom,
                        expectedSettlementDateTo,
                        actualSettlementDateFrom,
                        actualSettlementDateTo,
                        createdFrom,
                        createdTo
                ),
                pageable
        );
    }

    @GetMapping("/settlement-records/{id}")
    SettlementRecordResponse getSettlementRecord(@PathVariable UUID id) {
        return operationsQueryService.getSettlementRecord(id);
    }

    @GetMapping("/reconciliation-runs")
    Page<ReconciliationRunResponse> listReconciliationRuns(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) ReconciliationRunStatus status,
            @RequestParam(required = false) String sourceFile,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant completedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant completedTo,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return operationsQueryService.listReconciliationRuns(
                new ReconciliationRunOperationsFilter(id, status, sourceFile, startedFrom, startedTo, completedFrom, completedTo),
                pageable
        );
    }

    @GetMapping("/reconciliation-runs/{id}")
    ReconciliationRunResponse getReconciliationRun(@PathVariable UUID id) {
        return operationsQueryService.getReconciliationRun(id);
    }

    @GetMapping("/reconciliation-exceptions")
    Page<ReconciliationExceptionResponse> listReconciliationExceptions(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) UUID reconciliationRunId,
            @RequestParam(required = false) ReconciliationExceptionType exceptionType,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String providerTransactionReference,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return operationsQueryService.listReconciliationExceptions(
                new ReconciliationExceptionOperationsFilter(
                        id,
                        reconciliationRunId,
                        exceptionType,
                        provider,
                        providerTransactionReference,
                        createdFrom,
                        createdTo
                ),
                pageable
        );
    }

    @GetMapping("/reconciliation-exceptions/{id}")
    ReconciliationExceptionResponse getReconciliationException(@PathVariable UUID id) {
        return operationsQueryService.getReconciliationException(id);
    }
}
