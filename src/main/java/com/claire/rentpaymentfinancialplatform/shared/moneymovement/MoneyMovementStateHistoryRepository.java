package com.claire.rentpaymentfinancialplatform.shared.moneymovement;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoneyMovementStateHistoryRepository extends JpaRepository<MoneyMovementStateHistory, UUID> {

    List<MoneyMovementStateHistory> findByMoneyMovementIdOrderByChangedAtAsc(UUID moneyMovementId);
}
