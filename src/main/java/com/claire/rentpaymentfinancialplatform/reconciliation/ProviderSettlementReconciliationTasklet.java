package com.claire.rentpaymentfinancialplatform.reconciliation;

import com.claire.rentpaymentfinancialplatform.settlement.SettlementRecord;
import com.claire.rentpaymentfinancialplatform.settlement.SettlementRecordRepository;
import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationExceptionType;
import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationRunStatus;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
class ProviderSettlementReconciliationTasklet implements Tasklet {

    private final ProviderSettlementFileParser fileParser;
    private final ReconciliationRunRepository reconciliationRunRepository;
    private final ReconciliationExceptionRepository reconciliationExceptionRepository;
    private final SettlementRecordRepository settlementRecordRepository;

    ProviderSettlementReconciliationTasklet(
            ProviderSettlementFileParser fileParser,
            ReconciliationRunRepository reconciliationRunRepository,
            ReconciliationExceptionRepository reconciliationExceptionRepository,
            SettlementRecordRepository settlementRecordRepository
    ) {
        this.fileParser = fileParser;
        this.reconciliationRunRepository = reconciliationRunRepository;
        this.reconciliationExceptionRepository = reconciliationExceptionRepository;
        this.settlementRecordRepository = settlementRecordRepository;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        String sourceFile = chunkContext.getStepContext().getJobParameters().get("sourceFile").toString();
        ReconciliationRun existing = reconciliationRunRepository.findBySourceFile(sourceFile).orElse(null);
        if (existing != null && existing.getStatus() == ReconciliationRunStatus.COMPLETED) {
            return RepeatStatus.FINISHED;
        }
        ReconciliationRun run = existing == null
                ? reconciliationRunRepository.saveAndFlush(new ReconciliationRun(UUID.randomUUID(), sourceFile))
                : existing;

        try {
            ReconciliationResult result = reconcile(run, fileParser.parse(sourceFile));
            run.complete(result.totalRows(), result.matchedRows(), result.exceptionRows());
            reconciliationRunRepository.saveAndFlush(run);
            return RepeatStatus.FINISHED;
        } catch (RuntimeException exception) {
            run.fail(exception.getMessage());
            reconciliationRunRepository.saveAndFlush(run);
            throw exception;
        }
    }

    private ReconciliationResult reconcile(ReconciliationRun run, List<ProviderSettlementFileRow> rows) {
        int matchedRows = 0;
        int exceptionRows = 0;
        Set<String> seenProviderReferences = new HashSet<>();

        for (ProviderSettlementFileRow row : rows) {
            String providerReferenceKey = row.provider() + ":" + row.providerTransactionReference();
            if (!seenProviderReferences.add(providerReferenceKey)) {
                recordException(run, row, ReconciliationExceptionType.DUPLICATE_PROVIDER_RECORD, "Provider settlement file contains a duplicate provider transaction reference.");
                exceptionRows++;
                continue;
            }

            SettlementRecord settlement = settlementRecordRepository
                    .findByProviderAndProviderTransactionReference(row.provider(), row.providerTransactionReference())
                    .orElse(null);
            if (settlement == null) {
                recordException(run, row, ReconciliationExceptionType.MISSING_SETTLEMENT, "No expected settlement matched the provider transaction reference.");
                exceptionRows++;
                continue;
            }

            if (matchesExpectedAmounts(settlement, row)) {
                settlement.settle(row.grossAmount(), row.feeAmount(), row.netAmount(), row.settlementDate(), row.providerBatchReference());
                matchedRows++;
            } else {
                settlement.mismatch(row.grossAmount(), row.feeAmount(), row.netAmount(), row.settlementDate(), row.providerBatchReference());
                recordException(run, row, ReconciliationExceptionType.AMOUNT_MISMATCH, "Provider settlement amounts do not match expected settlement amounts.");
                exceptionRows++;
            }
            settlementRecordRepository.saveAndFlush(settlement);
        }
        return new ReconciliationResult(rows.size(), matchedRows, exceptionRows);
    }

    private void recordException(
            ReconciliationRun run,
            ProviderSettlementFileRow row,
            ReconciliationExceptionType exceptionType,
            String message
    ) {
        reconciliationExceptionRepository.saveAndFlush(new ReconciliationExceptionRecord(
                UUID.randomUUID(),
                run,
                exceptionType,
                row.provider(),
                row.providerTransactionReference(),
                message,
                row.rawRecord()
        ));
    }

    private static boolean matchesExpectedAmounts(SettlementRecord settlement, ProviderSettlementFileRow row) {
        return settlement.getCurrency().equals(row.currency())
                && settlement.getExpectedGrossAmount().compareTo(row.grossAmount()) == 0
                && settlement.getExpectedFeeAmount().compareTo(row.feeAmount()) == 0
                && settlement.getExpectedNetAmount().compareTo(row.netAmount()) == 0;
    }

    private record ReconciliationResult(int totalRows, int matchedRows, int exceptionRows) {
    }
}
