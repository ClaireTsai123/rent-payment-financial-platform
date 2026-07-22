package com.claire.rentpaymentfinancialplatform.reconciliation;

import java.util.List;
import java.util.UUID;
import com.claire.rentpaymentfinancialplatform.shared.domain.ReconciliationExceptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReconciliationExceptionRepository extends JpaRepository<ReconciliationExceptionRecord, UUID>, JpaSpecificationExecutor<ReconciliationExceptionRecord> {

    List<ReconciliationExceptionRecord> findByReconciliationRunId(UUID reconciliationRunId);

    @Query("""
            select count(e) > 0
            from ReconciliationExceptionRecord e
            where e.reconciliationRun.id = :runId
              and e.provider = :provider
              and e.providerTransactionReference = :providerTransactionReference
              and e.exceptionType = :exceptionType
            """)
    boolean existsForRunProviderReferenceAndType(
            @Param("runId") UUID runId,
            @Param("provider") String provider,
            @Param("providerTransactionReference") String providerTransactionReference,
            @Param("exceptionType") ReconciliationExceptionType exceptionType
    );
}
