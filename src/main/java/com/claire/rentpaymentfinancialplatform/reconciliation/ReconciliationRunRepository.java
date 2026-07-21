package com.claire.rentpaymentfinancialplatform.reconciliation;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, UUID> {

    Optional<ReconciliationRun> findBySourceFile(String sourceFile);
}
