package com.claire.rentpaymentfinancialplatform.reconciliation;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, UUID>, JpaSpecificationExecutor<ReconciliationRun> {

    Optional<ReconciliationRun> findBySourceFile(String sourceFile);
}
