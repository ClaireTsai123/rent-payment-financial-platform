package com.claire.rentpaymentfinancialplatform.shared.moneymovement;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoneyMovementRepository extends JpaRepository<MoneyMovement, UUID> {

    Optional<MoneyMovement> findByOperationKey(String operationKey);

    Page<MoneyMovement> findByPaymentPlanRenterId(String renterId, Pageable pageable);

    Page<MoneyMovement> findByPaymentPlanRenterIdAndPaymentPlanId(String renterId, UUID paymentPlanId, Pageable pageable);

    Optional<MoneyMovement> findByIdAndPaymentPlanRenterId(UUID id, String renterId);
}
