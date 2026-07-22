package com.claire.rentpaymentfinancialplatform.settlement;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, UUID>, JpaSpecificationExecutor<SettlementRecord> {

    @Query("select s from SettlementRecord s where s.moneyMovement.id = :moneyMovementId")
    Optional<SettlementRecord> findByMoneyMovementId(@Param("moneyMovementId") UUID moneyMovementId);

    Optional<SettlementRecord> findByProviderAndProviderTransactionReference(String provider, String providerTransactionReference);
}
