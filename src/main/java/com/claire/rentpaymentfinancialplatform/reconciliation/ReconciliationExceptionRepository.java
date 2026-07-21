package com.claire.rentpaymentfinancialplatform.reconciliation;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationExceptionRepository extends JpaRepository<ReconciliationExceptionRecord, UUID> {

    List<ReconciliationExceptionRecord> findByReconciliationRunId(UUID reconciliationRunId);
}
