package com.claire.rentpaymentfinancialplatform.operations;

import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.MoneyMovementDetail;
import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.MoneyMovementStateHistoryItem;
import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.MoneyMovementSummary;
import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.OutboxEventResponse;
import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.ProviderTransactionResponse;
import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.ProviderWebhookEventResponse;
import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.ReconciliationExceptionResponse;
import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.ReconciliationRunResponse;
import com.claire.rentpaymentfinancialplatform.operations.OperationsResponses.SettlementRecordResponse;
import com.claire.rentpaymentfinancialplatform.outbox.OutboxEvent;
import com.claire.rentpaymentfinancialplatform.outbox.OutboxEventRepository;
import com.claire.rentpaymentfinancialplatform.reconciliation.ReconciliationExceptionRecord;
import com.claire.rentpaymentfinancialplatform.reconciliation.ReconciliationExceptionRepository;
import com.claire.rentpaymentfinancialplatform.reconciliation.ReconciliationRun;
import com.claire.rentpaymentfinancialplatform.reconciliation.ReconciliationRunRepository;
import com.claire.rentpaymentfinancialplatform.settlement.SettlementRecord;
import com.claire.rentpaymentfinancialplatform.settlement.SettlementRecordRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovement;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementStateHistory;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementStateHistoryRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransaction;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransactionRepository;
import com.claire.rentpaymentfinancialplatform.webhook.ProviderWebhookEvent;
import com.claire.rentpaymentfinancialplatform.webhook.ProviderWebhookEventRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OperationsQueryService {

    private final MoneyMovementRepository moneyMovementRepository;
    private final MoneyMovementStateHistoryRepository stateHistoryRepository;
    private final ProviderTransactionRepository providerTransactionRepository;
    private final ProviderWebhookEventRepository providerWebhookEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final SettlementRecordRepository settlementRecordRepository;
    private final ReconciliationRunRepository reconciliationRunRepository;
    private final ReconciliationExceptionRepository reconciliationExceptionRepository;

    public OperationsQueryService(
            MoneyMovementRepository moneyMovementRepository,
            MoneyMovementStateHistoryRepository stateHistoryRepository,
            ProviderTransactionRepository providerTransactionRepository,
            ProviderWebhookEventRepository providerWebhookEventRepository,
            OutboxEventRepository outboxEventRepository,
            SettlementRecordRepository settlementRecordRepository,
            ReconciliationRunRepository reconciliationRunRepository,
            ReconciliationExceptionRepository reconciliationExceptionRepository
    ) {
        this.moneyMovementRepository = moneyMovementRepository;
        this.stateHistoryRepository = stateHistoryRepository;
        this.providerTransactionRepository = providerTransactionRepository;
        this.providerWebhookEventRepository = providerWebhookEventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.settlementRecordRepository = settlementRecordRepository;
        this.reconciliationRunRepository = reconciliationRunRepository;
        this.reconciliationExceptionRepository = reconciliationExceptionRepository;
    }

    public Page<MoneyMovementSummary> listMoneyMovements(MoneyMovementOperationsFilter filter, Pageable pageable) {
        validateInstantRange(filter.createdFrom(), filter.createdTo(), "createdFrom", "createdTo");
        Specification<MoneyMovement> spec = Specification.<MoneyMovement>where(equal("id", filter.id()))
                .and(equal("paymentPlan.id", filter.paymentPlanId()))
                .and(equal("paymentPlan.renterId", filter.renterId()))
                .and(equal("state", filter.state()))
                .and(equal("type", filter.type()))
                .and(equal("operationKey", filter.operationKey()))
                .and(instantRange("createdAt", filter.createdFrom(), filter.createdTo()));
        return moneyMovementRepository.findAll(spec, pageable).map(this::toMoneyMovementSummary);
    }

    public MoneyMovementDetail getMoneyMovement(UUID id) {
        MoneyMovement movement = moneyMovementRepository.findById(id)
                .orElseThrow(() -> new OperationsResourceNotFoundException("Money movement", id));
        return toMoneyMovementDetail(movement);
    }

    public Page<ProviderTransactionResponse> listProviderTransactions(ProviderTransactionOperationsFilter filter, Pageable pageable) {
        validateInstantRange(filter.createdFrom(), filter.createdTo(), "createdFrom", "createdTo");
        Specification<ProviderTransaction> spec = Specification.<ProviderTransaction>where(equal("id", filter.id()))
                .and(equal("moneyMovement.id", filter.moneyMovementId()))
                .and(equal("paymentAttempt.id", filter.paymentAttemptId()))
                .and(equal("provider", filter.provider()))
                .and(equal("normalizedStatus", filter.status()))
                .and(equal("providerTransactionId", filter.providerTransactionId()))
                .and(equal("providerIdempotencyKey", filter.providerIdempotencyKey()))
                .and(instantRange("createdAt", filter.createdFrom(), filter.createdTo()));
        return providerTransactionRepository.findAll(spec, pageable).map(this::toProviderTransaction);
    }

    public ProviderTransactionResponse getProviderTransaction(UUID id) {
        return providerTransactionRepository.findById(id)
                .map(this::toProviderTransaction)
                .orElseThrow(() -> new OperationsResourceNotFoundException("Provider transaction", id));
    }

    public Page<ProviderWebhookEventResponse> listProviderWebhookEvents(ProviderWebhookEventOperationsFilter filter, Pageable pageable) {
        validateInstantRange(filter.occurredFrom(), filter.occurredTo(), "occurredFrom", "occurredTo");
        validateInstantRange(filter.receivedFrom(), filter.receivedTo(), "receivedFrom", "receivedTo");
        Specification<ProviderWebhookEvent> spec = Specification.<ProviderWebhookEvent>where(equal("id", filter.id()))
                .and(equal("provider", filter.provider()))
                .and(equal("providerEventId", filter.providerEventId()))
                .and(equal("providerTransactionId", filter.providerTransactionId()))
                .and(equal("normalizedStatus", filter.normalizedStatus()))
                .and(equal("processingStatus", filter.status()))
                .and(instantRange("occurredAt", filter.occurredFrom(), filter.occurredTo()))
                .and(instantRange("receivedAt", filter.receivedFrom(), filter.receivedTo()));
        return providerWebhookEventRepository.findAll(spec, pageable).map(this::toProviderWebhookEvent);
    }

    public ProviderWebhookEventResponse getProviderWebhookEvent(UUID id) {
        return providerWebhookEventRepository.findById(id)
                .map(this::toProviderWebhookEvent)
                .orElseThrow(() -> new OperationsResourceNotFoundException("Provider webhook event", id));
    }

    public Page<OutboxEventResponse> listOutboxEvents(OutboxEventOperationsFilter filter, Pageable pageable) {
        validateInstantRange(filter.createdFrom(), filter.createdTo(), "createdFrom", "createdTo");
        Specification<OutboxEvent> spec = Specification.<OutboxEvent>where(equal("id", filter.id()))
                .and(equal("aggregateId", filter.aggregateId()))
                .and(equal("aggregateType", filter.aggregateType()))
                .and(equal("eventType", filter.eventType()))
                .and(equal("status", filter.status()))
                .and(instantRange("createdAt", filter.createdFrom(), filter.createdTo()));
        return outboxEventRepository.findAll(spec, pageable).map(this::toOutboxEvent);
    }

    public OutboxEventResponse getOutboxEvent(UUID id) {
        return outboxEventRepository.findById(id)
                .map(this::toOutboxEvent)
                .orElseThrow(() -> new OperationsResourceNotFoundException("Outbox event", id));
    }

    public Page<SettlementRecordResponse> listSettlementRecords(SettlementRecordOperationsFilter filter, Pageable pageable) {
        validateLocalDateRange(filter.expectedSettlementDateFrom(), filter.expectedSettlementDateTo(), "expectedSettlementDateFrom", "expectedSettlementDateTo");
        validateLocalDateRange(filter.actualSettlementDateFrom(), filter.actualSettlementDateTo(), "actualSettlementDateFrom", "actualSettlementDateTo");
        validateInstantRange(filter.createdFrom(), filter.createdTo(), "createdFrom", "createdTo");
        Specification<SettlementRecord> spec = Specification.<SettlementRecord>where(equal("id", filter.id()))
                .and(equal("moneyMovement.id", filter.moneyMovementId()))
                .and(equal("providerTransaction.id", filter.providerTransactionId()))
                .and(equal("provider", filter.provider()))
                .and(equal("status", filter.status()))
                .and(equal("providerTransactionReference", filter.providerTransactionReference()))
                .and(equal("providerBatchReference", filter.providerBatchReference()))
                .and(localDateRange("expectedSettlementDate", filter.expectedSettlementDateFrom(), filter.expectedSettlementDateTo()))
                .and(localDateRange("actualSettlementDate", filter.actualSettlementDateFrom(), filter.actualSettlementDateTo()))
                .and(instantRange("createdAt", filter.createdFrom(), filter.createdTo()));
        return settlementRecordRepository.findAll(spec, pageable).map(this::toSettlementRecord);
    }

    public SettlementRecordResponse getSettlementRecord(UUID id) {
        return settlementRecordRepository.findById(id)
                .map(this::toSettlementRecord)
                .orElseThrow(() -> new OperationsResourceNotFoundException("Settlement record", id));
    }

    public Page<ReconciliationRunResponse> listReconciliationRuns(ReconciliationRunOperationsFilter filter, Pageable pageable) {
        validateInstantRange(filter.startedFrom(), filter.startedTo(), "startedFrom", "startedTo");
        validateInstantRange(filter.completedFrom(), filter.completedTo(), "completedFrom", "completedTo");
        Specification<ReconciliationRun> spec = Specification.<ReconciliationRun>where(equal("id", filter.id()))
                .and(equal("status", filter.status()))
                .and(equal("sourceFile", filter.sourceFile()))
                .and(instantRange("startedAt", filter.startedFrom(), filter.startedTo()))
                .and(instantRange("completedAt", filter.completedFrom(), filter.completedTo()));
        return reconciliationRunRepository.findAll(spec, pageable).map(this::toReconciliationRun);
    }

    public ReconciliationRunResponse getReconciliationRun(UUID id) {
        return reconciliationRunRepository.findById(id)
                .map(this::toReconciliationRun)
                .orElseThrow(() -> new OperationsResourceNotFoundException("Reconciliation run", id));
    }

    public Page<ReconciliationExceptionResponse> listReconciliationExceptions(ReconciliationExceptionOperationsFilter filter, Pageable pageable) {
        validateInstantRange(filter.createdFrom(), filter.createdTo(), "createdFrom", "createdTo");
        Specification<ReconciliationExceptionRecord> spec = Specification.<ReconciliationExceptionRecord>where(equal("id", filter.id()))
                .and(equal("reconciliationRun.id", filter.reconciliationRunId()))
                .and(equal("exceptionType", filter.exceptionType()))
                .and(equal("provider", filter.provider()))
                .and(equal("providerTransactionReference", filter.providerTransactionReference()))
                .and(instantRange("createdAt", filter.createdFrom(), filter.createdTo()));
        return reconciliationExceptionRepository.findAll(spec, pageable).map(this::toReconciliationException);
    }

    public ReconciliationExceptionResponse getReconciliationException(UUID id) {
        return reconciliationExceptionRepository.findById(id)
                .map(this::toReconciliationException)
                .orElseThrow(() -> new OperationsResourceNotFoundException("Reconciliation exception", id));
    }

    private MoneyMovementSummary toMoneyMovementSummary(MoneyMovement movement) {
        return new MoneyMovementSummary(
                movement.getId(),
                movement.getPaymentPlan().getId(),
                movement.getPaymentPlan().getRenterId(),
                movement.getType(),
                movement.getState(),
                movement.getAmount(),
                movement.getCurrency(),
                movement.getOperationKey(),
                movement.getCreatedAt(),
                movement.getUpdatedAt()
        );
    }

    private MoneyMovementDetail toMoneyMovementDetail(MoneyMovement movement) {
        return new MoneyMovementDetail(
                movement.getId(),
                movement.getPaymentPlan().getId(),
                movement.getPaymentPlan().getRenterId(),
                movement.getType(),
                movement.getState(),
                movement.getAmount(),
                movement.getCurrency(),
                movement.getOperationKey(),
                movement.getVersion(),
                movement.getCreatedAt(),
                movement.getUpdatedAt(),
                stateHistoryRepository.findByMoneyMovementIdOrderByChangedAtAsc(movement.getId()).stream()
                        .map(this::toStateHistoryItem)
                        .toList()
        );
    }

    private MoneyMovementStateHistoryItem toStateHistoryItem(MoneyMovementStateHistory history) {
        return new MoneyMovementStateHistoryItem(
                history.getId(),
                history.getFromState(),
                history.getToState(),
                history.getReason(),
                history.getChangedAt()
        );
    }

    private ProviderTransactionResponse toProviderTransaction(ProviderTransaction transaction) {
        return new ProviderTransactionResponse(
                transaction.getId(),
                transaction.getMoneyMovement().getId(),
                transaction.getPaymentAttempt().getId(),
                transaction.getProvider(),
                transaction.getProviderTransactionId(),
                transaction.getProviderIdempotencyKey(),
                transaction.getNormalizedStatus(),
                transaction.getRawStatus(),
                transaction.getSettlementReference(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }

    private ProviderWebhookEventResponse toProviderWebhookEvent(ProviderWebhookEvent event) {
        return new ProviderWebhookEventResponse(
                event.getId(),
                event.getProvider(),
                event.getProviderEventId(),
                event.getProviderTransactionId(),
                event.getNormalizedStatus(),
                event.getRawPayload(),
                event.getProcessingStatus(),
                event.getFailureReason(),
                event.getOccurredAt(),
                event.getReceivedAt(),
                event.getProcessedAt()
        );
    }

    private OutboxEventResponse toOutboxEvent(OutboxEvent event) {
        return new OutboxEventResponse(
                event.getId(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                event.getPayload(),
                event.getStatus(),
                event.getAttempts(),
                event.getLastError(),
                event.getPublishedAt(),
                event.getNextAttemptAt(),
                event.getVersion(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private SettlementRecordResponse toSettlementRecord(SettlementRecord settlement) {
        return new SettlementRecordResponse(
                settlement.getId(),
                settlement.getMoneyMovementId(),
                settlement.getProviderTransactionId(),
                settlement.getStatus(),
                settlement.getExpectedGrossAmount(),
                settlement.getExpectedFeeAmount(),
                settlement.getExpectedNetAmount(),
                settlement.getActualGrossAmount(),
                settlement.getActualFeeAmount(),
                settlement.getActualNetAmount(),
                settlement.getCurrency(),
                settlement.getExpectedSettlementDate(),
                settlement.getActualSettlementDate(),
                settlement.getProvider(),
                settlement.getProviderTransactionReference(),
                settlement.getProviderBatchReference(),
                settlement.getCreatedAt(),
                settlement.getUpdatedAt()
        );
    }

    private ReconciliationRunResponse toReconciliationRun(ReconciliationRun run) {
        return new ReconciliationRunResponse(
                run.getId(),
                run.getSourceFile(),
                run.getStatus(),
                run.getTotalRows(),
                run.getMatchedRows(),
                run.getExceptionRows(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getFailureReason()
        );
    }

    private ReconciliationExceptionResponse toReconciliationException(ReconciliationExceptionRecord exception) {
        return new ReconciliationExceptionResponse(
                exception.getId(),
                exception.getReconciliationRun().getId(),
                exception.getExceptionType(),
                exception.getProvider(),
                exception.getProviderTransactionReference(),
                exception.getMessage(),
                exception.getRawRecord(),
                exception.getCreatedAt()
        );
    }

    private static <T> Specification<T> equal(String path, Object value) {
        if (value == null || (value instanceof String text && text.isBlank())) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(path(root, path), value);
    }

    private static <T> Specification<T> instantRange(String path, Instant from, Instant to) {
        if (from == null && to == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> {
            if (from != null && to != null) {
                return criteriaBuilder.between(path(root, path), from, to);
            }
            if (from != null) {
                return criteriaBuilder.greaterThanOrEqualTo(path(root, path), from);
            }
            return criteriaBuilder.lessThanOrEqualTo(path(root, path), to);
        };
    }

    private static <T> Specification<T> localDateRange(String path, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> {
            if (from != null && to != null) {
                return criteriaBuilder.between(path(root, path), from, to);
            }
            if (from != null) {
                return criteriaBuilder.greaterThanOrEqualTo(path(root, path), from);
            }
            return criteriaBuilder.lessThanOrEqualTo(path(root, path), to);
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> jakarta.persistence.criteria.Path<T> path(jakarta.persistence.criteria.Path<?> root, String path) {
        jakarta.persistence.criteria.Path<?> current = root;
        for (String part : path.split("\\.")) {
            current = current.get(part);
        }
        return (jakarta.persistence.criteria.Path<T>) current;
    }

    private static void validateInstantRange(Instant from, Instant to, String fromName, String toName) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new OperationsInvalidFilterException(fromName + " must be before or equal to " + toName + ".");
        }
    }

    private static void validateLocalDateRange(LocalDate from, LocalDate to, String fromName, String toName) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new OperationsInvalidFilterException(fromName + " must be before or equal to " + toName + ".");
        }
    }
}
