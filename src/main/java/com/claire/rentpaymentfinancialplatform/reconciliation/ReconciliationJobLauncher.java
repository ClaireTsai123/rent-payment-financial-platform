package com.claire.rentpaymentfinancialplatform.reconciliation;

import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationRunStatus;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Service
public class ReconciliationJobLauncher {

    private final JobLauncher jobLauncher;
    private final Job providerSettlementReconciliationJob;
    private final ReconciliationRunRepository reconciliationRunRepository;

    public ReconciliationJobLauncher(
            JobLauncher jobLauncher,
            Job providerSettlementReconciliationJob,
            ReconciliationRunRepository reconciliationRunRepository
    ) {
        this.jobLauncher = jobLauncher;
        this.providerSettlementReconciliationJob = providerSettlementReconciliationJob;
        this.reconciliationRunRepository = reconciliationRunRepository;
    }

    public ReconciliationRun reconcileProviderSettlementFile(String sourceFile) {
        ReconciliationRun existing = reconciliationRunRepository.findBySourceFile(sourceFile).orElse(null);
        if (existing != null && existing.getStatus() == ReconciliationRunStatus.COMPLETED) {
            return existing;
        }
        try {
            JobExecution jobExecution = jobLauncher.run(providerSettlementReconciliationJob, new JobParametersBuilder()
                    .addString("sourceFile", sourceFile)
                    .toJobParameters());
            if (jobExecution.getStatus() == BatchStatus.FAILED) {
                throw new IllegalStateException("Provider settlement reconciliation job failed.");
            }
            return reconciliationRunRepository.findBySourceFile(sourceFile)
                    .orElseThrow(() -> new IllegalStateException("Reconciliation run was not recorded."));
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("Provider settlement reconciliation job failed.", exception);
        }
    }
}
