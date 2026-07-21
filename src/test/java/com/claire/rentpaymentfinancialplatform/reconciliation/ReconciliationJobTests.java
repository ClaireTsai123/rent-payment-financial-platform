package com.claire.rentpaymentfinancialplatform.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;

import com.claire.rentpaymentfinancialplatform.PostgresIntegrationTest;
import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyRecordRepository;
import com.claire.rentpaymentfinancialplatform.outbox.OutboxEventRepository;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlan;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlanRepository;
import com.claire.rentpaymentfinancialplatform.settlement.SettlementRecord;
import com.claire.rentpaymentfinancialplatform.settlement.SettlementRecordRepository;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType;
import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentAttemptStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentPlanStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderTransactionStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationExceptionType;
import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationRunStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.SettlementStatus;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovement;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementStateHistoryRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.PaymentAttempt;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.PaymentAttemptRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransaction;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransactionRepository;
import com.claire.rentpaymentfinancialplatform.webhook.ProviderWebhookEventRepository;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ReconciliationJobTests extends PostgresIntegrationTest {

    private static final String HEADER = "provider,providerTransactionReference,grossAmount,feeAmount,netAmount,currency,settlementDate,providerBatchReference";

    @Autowired
    private ReconciliationJobLauncher reconciliationJobLauncher;

    @Autowired
    private ReconciliationRunRepository reconciliationRunRepository;

    @Autowired
    private ReconciliationExceptionRepository reconciliationExceptionRepository;

    @Autowired
    private SettlementRecordRepository settlementRecordRepository;

    @Autowired
    private ProviderTransactionRepository providerTransactionRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private MoneyMovementRepository moneyMovementRepository;

    @Autowired
    private PaymentPlanRepository paymentPlanRepository;

    @Autowired
    private ProviderWebhookEventRepository webhookEventRepository;

    @Autowired
    private MoneyMovementStateHistoryRepository stateHistoryRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void cleanDatabase() {
        reconciliationExceptionRepository.deleteAll();
        reconciliationRunRepository.deleteAll();
        webhookEventRepository.deleteAll();
        settlementRecordRepository.deleteAll();
        providerTransactionRepository.deleteAll();
        paymentAttemptRepository.deleteAll();
        stateHistoryRepository.deleteAll();
        outboxEventRepository.deleteAll();
        moneyMovementRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        paymentPlanRepository.deleteAll();
    }

    @Test
    void matchesExpectedSettlementAndMarksItSettled() throws Exception {
        SettlementRecord settlement = seedExpectedSettlement("provider-txn-" + UUID.randomUUID(), "100.00", "1.50", "98.50");
        Path sourceFile = settlementFile("matched.csv", row(settlement, "100.00", "1.50", "98.50"));

        ReconciliationRun run = reconciliationJobLauncher.reconcileProviderSettlementFile(sourceFile.toString());

        assertThat(run.getStatus()).isEqualTo(ReconciliationRunStatus.COMPLETED);
        assertThat(run.getTotalRows()).isEqualTo(1);
        assertThat(run.getMatchedRows()).isEqualTo(1);
        assertThat(run.getExceptionRows()).isZero();
        assertThat(reconciliationExceptionRepository.findAll()).isEmpty();
        assertThat(settlementRecordRepository.findById(settlement.getId())).get().satisfies(reloaded -> {
            assertThat(reloaded.getStatus()).isEqualTo(SettlementStatus.SETTLED);
            assertThat(reloaded.getActualGrossAmount()).isEqualByComparingTo("100.00");
            assertThat(reloaded.getActualFeeAmount()).isEqualByComparingTo("1.50");
            assertThat(reloaded.getActualNetAmount()).isEqualByComparingTo("98.50");
            assertThat(reloaded.getProviderBatchReference()).isEqualTo("batch-001");
        });
    }

    @Test
    void recordsMissingSettlementException() throws Exception {
        Path sourceFile = settlementFile(
                "missing.csv",
                "mock-provider,missing-provider-txn,100.00,0.00,100.00,USD,2026-08-02,batch-001"
        );

        ReconciliationRun run = reconciliationJobLauncher.reconcileProviderSettlementFile(sourceFile.toString());

        assertThat(run.getStatus()).isEqualTo(ReconciliationRunStatus.COMPLETED);
        assertThat(run.getMatchedRows()).isZero();
        assertThat(run.getExceptionRows()).isEqualTo(1);
        assertThat(reconciliationExceptionRepository.findAll()).singleElement().satisfies(exception -> {
            assertThat(exception.getExceptionType()).isEqualTo(ReconciliationExceptionType.MISSING_SETTLEMENT);
            assertThat(exception.getProviderTransactionReference()).isEqualTo("missing-provider-txn");
        });
    }

    @Test
    void recordsAmountMismatchAndMarksSettlementMismatched() throws Exception {
        SettlementRecord settlement = seedExpectedSettlement("provider-txn-" + UUID.randomUUID(), "100.00", "0.00", "100.00");
        Path sourceFile = settlementFile("mismatch.csv", row(settlement, "101.00", "0.00", "101.00"));

        ReconciliationRun run = reconciliationJobLauncher.reconcileProviderSettlementFile(sourceFile.toString());

        assertThat(run.getMatchedRows()).isZero();
        assertThat(run.getExceptionRows()).isEqualTo(1);
        assertThat(settlementRecordRepository.findById(settlement.getId())).get()
                .satisfies(reloaded -> assertThat(reloaded.getStatus()).isEqualTo(SettlementStatus.MISMATCHED));
        assertThat(reconciliationExceptionRepository.findAll()).singleElement()
                .satisfies(exception -> assertThat(exception.getExceptionType()).isEqualTo(ReconciliationExceptionType.AMOUNT_MISMATCH));
    }

    @Test
    void recordsDuplicateProviderRecordWithoutApplyingSecondUpdate() throws Exception {
        SettlementRecord settlement = seedExpectedSettlement("provider-txn-" + UUID.randomUUID(), "100.00", "0.00", "100.00");
        Path sourceFile = settlementFile(
                "duplicate.csv",
                row(settlement, "100.00", "0.00", "100.00"),
                row(settlement, "100.00", "0.00", "100.00")
        );

        ReconciliationRun run = reconciliationJobLauncher.reconcileProviderSettlementFile(sourceFile.toString());

        assertThat(run.getMatchedRows()).isEqualTo(1);
        assertThat(run.getExceptionRows()).isEqualTo(1);
        assertThat(settlementRecordRepository.findById(settlement.getId())).get()
                .satisfies(reloaded -> assertThat(reloaded.getStatus()).isEqualTo(SettlementStatus.SETTLED));
        assertThat(reconciliationExceptionRepository.findAll()).singleElement()
                .satisfies(exception -> assertThat(exception.getExceptionType()).isEqualTo(ReconciliationExceptionType.DUPLICATE_PROVIDER_RECORD));
    }

    @Test
    void completedSourceFileRerunDoesNotDuplicateResults() throws Exception {
        SettlementRecord settlement = seedExpectedSettlement("provider-txn-" + UUID.randomUUID(), "100.00", "0.00", "100.00");
        Path sourceFile = settlementFile("rerun.csv", row(settlement, "100.00", "0.00", "100.00"));

        ReconciliationRun firstRun = reconciliationJobLauncher.reconcileProviderSettlementFile(sourceFile.toString());
        ReconciliationRun secondRun = reconciliationJobLauncher.reconcileProviderSettlementFile(sourceFile.toString());

        assertThat(secondRun.getId()).isEqualTo(firstRun.getId());
        assertThat(reconciliationRunRepository.findAll()).hasSize(1);
        assertThat(reconciliationExceptionRepository.findAll()).isEmpty();
        assertThat(settlementRecordRepository.findAll()).hasSize(1);
    }

    private SettlementRecord seedExpectedSettlement(String providerTransactionReference, String gross, String fee, String net) {
        PaymentPlan paymentPlan = paymentPlanRepository.saveAndFlush(new PaymentPlan(
                UUID.randomUUID(),
                "renter-" + UUID.randomUUID(),
                "billing-" + UUID.randomUUID(),
                new BigDecimal("2500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("2000.00"),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 15),
                PaymentPlanStatus.ACTIVE
        ));
        MoneyMovement moneyMovement = moneyMovementRepository.saveAndFlush(new MoneyMovement(
                UUID.randomUUID(),
                paymentPlan,
                MoneyMovementType.RENTER_COLLECTION,
                MoneyMovementState.SUCCEEDED,
                new BigDecimal(gross),
                "USD",
                "movement-" + UUID.randomUUID()
        ));
        PaymentAttempt attempt = paymentAttemptRepository.saveAndFlush(new PaymentAttempt(
                UUID.randomUUID(),
                moneyMovement,
                1,
                PaymentAttemptStatus.SUCCEEDED
        ));
        ProviderTransaction providerTransaction = providerTransactionRepository.saveAndFlush(new ProviderTransaction(
                UUID.randomUUID(),
                moneyMovement,
                attempt,
                "mock-provider",
                providerTransactionReference,
                "provider-key-" + UUID.randomUUID(),
                ProviderTransactionStatus.SUCCEEDED,
                "SUCCEEDED"
        ));
        return settlementRecordRepository.saveAndFlush(new SettlementRecord(
                UUID.randomUUID(),
                moneyMovement,
                providerTransaction,
                SettlementStatus.EXPECTED,
                new BigDecimal(gross),
                new BigDecimal(fee),
                new BigDecimal(net),
                "USD",
                LocalDate.of(2026, 8, 2),
                providerTransaction.getProvider(),
                providerTransaction.getProviderTransactionId()
        ));
    }

    private Path settlementFile(String fileName, String... rows) throws Exception {
        Path sourceFile = tempDir.resolve(fileName);
        Files.writeString(sourceFile, HEADER + System.lineSeparator() + String.join(System.lineSeparator(), rows));
        return sourceFile;
    }

    private static String row(SettlementRecord settlement, String gross, String fee, String net) {
        return "%s,%s,%s,%s,%s,%s,%s,%s".formatted(
                settlement.getProvider(),
                settlement.getProviderTransactionReference(),
                gross,
                fee,
                net,
                settlement.getCurrency(),
                LocalDate.of(2026, 8, 2),
                "batch-001"
        );
    }
}
