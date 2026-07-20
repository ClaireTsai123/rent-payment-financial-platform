package com.claire.rentpaymentfinancialplatform.shared.moneymovement;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderTransactionRepository extends JpaRepository<ProviderTransaction, UUID> {

    Optional<ProviderTransaction> findByProviderAndProviderTransactionId(String provider, String providerTransactionId);

    @Query("""
            select p
            from ProviderTransaction p
            where p.moneyMovement.id = :moneyMovementId
            order by p.createdAt desc
            limit 1
            """)
    Optional<ProviderTransaction> findLatestByMoneyMovementId(@Param("moneyMovementId") UUID moneyMovementId);
}
