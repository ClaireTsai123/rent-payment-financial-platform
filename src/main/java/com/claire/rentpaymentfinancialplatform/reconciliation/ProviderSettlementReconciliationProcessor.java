package com.claire.rentpaymentfinancialplatform.reconciliation;

import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationExceptionType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;

class ProviderSettlementReconciliationProcessor implements ItemProcessor<ProviderSettlementFileRow, ProviderSettlementReconciliationItem>, StepExecutionListener {

    static final String SEEN_PROVIDER_REFERENCES_KEY = "seenProviderReferenceKeys";

    private final Set<String> seenProviderReferences = new HashSet<>();
    private ExecutionContext executionContext;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.executionContext = stepExecution.getExecutionContext();
        String persistedKeys = executionContext.getString(SEEN_PROVIDER_REFERENCES_KEY, "");
        if (!persistedKeys.isBlank()) {
            seenProviderReferences.addAll(Arrays.asList(persistedKeys.split("\n")));
        }
    }

    @Override
    public ProviderSettlementReconciliationItem process(ProviderSettlementFileRow row) {
        if (row.blank()) {
            return null;
        }
        String providerReferenceKey = row.provider() + ":" + row.providerTransactionReference();
        if (!seenProviderReferences.add(providerReferenceKey)) {
            return ProviderSettlementReconciliationItem.exception(
                    row,
                    ReconciliationExceptionType.DUPLICATE_PROVIDER_RECORD,
                    "Provider settlement file contains a duplicate provider transaction reference."
            );
        }
        executionContext.putString(
                SEEN_PROVIDER_REFERENCES_KEY,
                seenProviderReferences.stream().sorted().collect(Collectors.joining("\n"))
        );
        return ProviderSettlementReconciliationItem.processable(row);
    }
}
