package com.claire.rentpaymentfinancialplatform.reconciliation;

import com.claire.rentpaymentfinancialplatform.settlement.SettlementRecordRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(ReconciliationJobConfiguration.ReconciliationJobProperties.class)
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
            FlatFileItemReader<ProviderSettlementFileRow> providerSettlementFileReader,
            ProviderSettlementReconciliationProcessor providerSettlementReconciliationProcessor,
            ProviderSettlementReconciliationWriter providerSettlementReconciliationWriter,
            ReconciliationJobProperties properties
    ) {
        return new StepBuilder("providerSettlementReconciliationStep", jobRepository)
                .<ProviderSettlementFileRow, ProviderSettlementReconciliationItem>chunk(properties.chunkSize(), transactionManager)
                .reader(providerSettlementFileReader)
                .processor(providerSettlementReconciliationProcessor)
                .writer(providerSettlementReconciliationWriter)
                .listener(providerSettlementReconciliationProcessor)
                .listener(providerSettlementReconciliationWriter)
                .build();
    }

    @Bean
    @StepScope
    FlatFileItemReader<ProviderSettlementFileRow> providerSettlementFileReader(
            @Value("#{jobParameters['sourceFile']}") String sourceFile,
            SettlementFileSource settlementFileSource,
            ProviderSettlementFileParser parser
    ) {
        return new FlatFileItemReaderBuilder<ProviderSettlementFileRow>()
                .name("providerSettlementFileReader")
                .resource(settlementFileSource.resourceFor(sourceFile))
                .linesToSkip(1)
                .skippedLinesCallback(parser::validateHeader)
                .lineMapper((line, lineNumber) -> parser.parseLine(line))
                .build();
    }

    @Bean
    @StepScope
    ProviderSettlementReconciliationProcessor providerSettlementReconciliationProcessor() {
        return new ProviderSettlementReconciliationProcessor();
    }

    @Bean
    @StepScope
    ProviderSettlementReconciliationWriter providerSettlementReconciliationWriter(
            @Value("#{jobParameters['sourceFile']}") String sourceFile,
            ReconciliationRunRepository reconciliationRunRepository,
            ReconciliationExceptionRepository reconciliationExceptionRepository,
            SettlementRecordRepository settlementRecordRepository
    ) {
        return new ProviderSettlementReconciliationWriter(
                sourceFile,
                reconciliationRunRepository,
                reconciliationExceptionRepository,
                settlementRecordRepository
        );
    }

    @ConfigurationProperties(prefix = "reconciliation.settlement")
    public record ReconciliationJobProperties(int chunkSize) {

        public ReconciliationJobProperties {
            if (chunkSize <= 0) {
                chunkSize = 50;
            }
        }
    }
}
