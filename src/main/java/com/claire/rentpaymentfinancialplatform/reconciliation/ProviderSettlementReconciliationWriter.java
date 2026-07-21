package com.claire.rentpaymentfinancialplatform.reconciliation;

import com.claire.rentpaymentfinancialplatform.settlement.SettlementRecord;
import com.claire.rentpaymentfinancialplatform.settlement.SettlementRecordRepository;
import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationExceptionType;
import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationRunStatus;
import java.util.UUID;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;

class ProviderSettlementReconciliationWriter implements ItemWriter<ProviderSettlementReconciliationItem>, StepExecutionListener {

    private static final String RUN_ID_KEY = "reconciliationRunId";
    private static final String TOTAL_ROWS_KEY = "totalRows";
    private static final String MATCHED_ROWS_KEY = "matchedRows";
    private static final String EXCEPTION_ROWS_KEY = "exceptionRows";

    private final String sourceFile;
    private final ReconciliationRunRepository reconciliationRunRepository;
    private final ReconciliationExceptionRepository reconciliationExceptionRepository;
    private final SettlementRecordRepository settlementRecordRepository;

    private ExecutionContext executionContext;

    ProviderSettlementReconciliationWriter(
            String sourceFile,
            ReconciliationRunRepository reconciliationRunRepository,
            ReconciliationExceptionRepository reconciliationExceptionRepository,
            SettlementRecordRepository settlementRecordRepository
    ) {
        this.sourceFile = sourceFile;
        this.reconciliationRunRepository = reconciliationRunRepository;
        this.reconciliationExceptionRepository = reconciliationExceptionRepository;
        this.settlementRecordRepository = settlementRecordRepository;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.executionContext = stepExecution.getExecutionContext();
        ReconciliationRun run = reconciliationRunRepository.findBySourceFile(sourceFile).orElse(null);
        if (run == null) {
            run = reconciliationRunRepository.saveAndFlush(new ReconciliationRun(UUID.randomUUID(), sourceFile));
        } else if (run.getStatus() != ReconciliationRunStatus.COMPLETED) {
            run.restart();
            reconciliationRunRepository.saveAndFlush(run);
        }
        executionContext.putString(RUN_ID_KEY, run.getId().toString());
        if (!executionContext.containsKey(TOTAL_ROWS_KEY)) {
            executionContext.putInt(TOTAL_ROWS_KEY, run.getTotalRows());
            executionContext.putInt(MATCHED_ROWS_KEY, run.getMatchedRows());
            executionContext.putInt(EXCEPTION_ROWS_KEY, run.getExceptionRows());
        }
    }

    @Override
    public void write(Chunk<? extends ProviderSettlementReconciliationItem> chunk) {
        ReconciliationRun run = currentRun();
        int totalRows = executionContext.getInt(TOTAL_ROWS_KEY, 0);
        int matchedRows = executionContext.getInt(MATCHED_ROWS_KEY, 0);
        int exceptionRows = executionContext.getInt(EXCEPTION_ROWS_KEY, 0);

        for (ProviderSettlementReconciliationItem item : chunk.getItems()) {
            totalRows++;
            if (item.hasException()) {
                recordExceptionIfAbsent(run, item.row(), item.exceptionType(), item.exceptionMessage());
                exceptionRows++;
                continue;
            }

            ProviderSettlementFileRow row = item.row();
            SettlementRecord settlement = settlementRecordRepository
                    .findByProviderAndProviderTransactionReference(row.provider(), row.providerTransactionReference())
                    .orElse(null);
            if (settlement == null) {
                recordExceptionIfAbsent(run, row, ReconciliationExceptionType.MISSING_SETTLEMENT, "No expected settlement matched the provider transaction reference.");
                exceptionRows++;
                continue;
            }

            if (matchesExpectedAmounts(settlement, row)) {
                settlement.settle(row.grossAmount(), row.feeAmount(), row.netAmount(), row.settlementDate(), row.providerBatchReference());
                matchedRows++;
            } else {
                settlement.mismatch(row.grossAmount(), row.feeAmount(), row.netAmount(), row.settlementDate(), row.providerBatchReference());
                recordExceptionIfAbsent(run, row, ReconciliationExceptionType.AMOUNT_MISMATCH, "Provider settlement amounts do not match expected settlement amounts.");
                exceptionRows++;
            }
            settlementRecordRepository.save(settlement);
        }

        executionContext.putInt(TOTAL_ROWS_KEY, totalRows);
        executionContext.putInt(MATCHED_ROWS_KEY, matchedRows);
        executionContext.putInt(EXCEPTION_ROWS_KEY, exceptionRows);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        ReconciliationRun run = currentRun();
        if (stepExecution.getStatus().isUnsuccessful()) {
            run.fail(failureReason(stepExecution));
        } else {
            run.complete(
                    executionContext.getInt(TOTAL_ROWS_KEY, 0),
                    executionContext.getInt(MATCHED_ROWS_KEY, 0),
                    executionContext.getInt(EXCEPTION_ROWS_KEY, 0)
            );
        }
        reconciliationRunRepository.saveAndFlush(run);
        return stepExecution.getExitStatus();
    }

    private ReconciliationRun currentRun() {
        UUID runId = UUID.fromString(executionContext.getString(RUN_ID_KEY));
        return reconciliationRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalStateException("Reconciliation run was not recorded."));
    }

    private void recordExceptionIfAbsent(
            ReconciliationRun run,
            ProviderSettlementFileRow row,
            ReconciliationExceptionType exceptionType,
            String message
    ) {
        boolean exists = reconciliationExceptionRepository.existsForRunProviderReferenceAndType(
                run.getId(),
                row.provider(),
                row.providerTransactionReference(),
                exceptionType
        );
        if (!exists) {
            reconciliationExceptionRepository.save(new ReconciliationExceptionRecord(
                    UUID.randomUUID(),
                    run,
                    exceptionType,
                    row.provider(),
                    row.providerTransactionReference(),
                    message,
                    row.rawRecord()
            ));
        }
    }

    private static boolean matchesExpectedAmounts(SettlementRecord settlement, ProviderSettlementFileRow row) {
        return settlement.getCurrency().equals(row.currency())
                && settlement.getExpectedGrossAmount().compareTo(row.grossAmount()) == 0
                && settlement.getExpectedFeeAmount().compareTo(row.feeAmount()) == 0
                && settlement.getExpectedNetAmount().compareTo(row.netAmount()) == 0;
    }

    private static String failureReason(StepExecution stepExecution) {
        if (!stepExecution.getFailureExceptions().isEmpty()) {
            return truncate(rootCause(stepExecution.getFailureExceptions().get(0)).getMessage());
        }
        return truncate(stepExecution.getExitStatus().getExitDescription());
    }

    private static Throwable rootCause(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "Provider settlement reconciliation job failed.";
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
