package com.claire.rentpaymentfinancialplatform.shared.moneymovement;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderTransactionRepository extends JpaRepository<ProviderTransaction, UUID> {

    Optional<ProviderTransaction> findByProviderAndProviderTransactionId(String provider, String providerTransactionId);
}
