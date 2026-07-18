package com.claire.rentpaymentfinancialplatform.shared.moneymovement;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoneyMovementRepository extends JpaRepository<MoneyMovement, UUID> {

    Optional<MoneyMovement> findByOperationKey(String operationKey);
}
