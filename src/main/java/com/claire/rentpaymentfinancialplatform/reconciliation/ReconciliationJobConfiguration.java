package com.claire.rentpaymentfinancialplatform.reconciliation;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ReconciliationJobConfiguration {

    public static final String JOB_NAME = "providerSettlementReconciliationJob";

    @Bean
    Job providerSettlementReconciliationJob(JobRepository jobRepository, Step providerSettlementReconciliationStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(providerSettlementReconciliationStep)
                .build();
    }

    @Bean
    Step providerSettlementReconciliationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ProviderSettlementReconciliationTasklet tasklet
    ) {
        return new StepBuilder("providerSettlementReconciliationStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
