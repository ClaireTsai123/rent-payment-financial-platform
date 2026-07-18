package com.claire.rentpaymentfinancialplatform.collection;

import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlan;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlanRepository;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovement;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RenterCollectionService {

    private final PaymentPlanRepository paymentPlanRepository;
    private final MoneyMovementRepository moneyMovementRepository;

    public RenterCollectionService(
            PaymentPlanRepository paymentPlanRepository,
            MoneyMovementRepository moneyMovementRepository
    ) {
        this.paymentPlanRepository = paymentPlanRepository;
        this.moneyMovementRepository = moneyMovementRepository;
    }

    @Transactional
    public RenterCollectionResponse createCollection(CreateRenterCollectionRequest request) {
        PaymentPlan paymentPlan = paymentPlanRepository.findById(request.paymentPlanId())
                .orElseThrow(() -> new PaymentPlanNotFoundException(request.paymentPlanId()));

        MoneyMovement moneyMovement = moneyMovementRepository.saveAndFlush(new MoneyMovement(
                UUID.randomUUID(),
                paymentPlan,
                MoneyMovementType.RENTER_COLLECTION,
                MoneyMovementState.CREATED,
                paymentPlan.getInitialCollectionAmount(),
                request.currency(),
                request.operationKey()
        ));

        return toResponse(moneyMovement);
    }

    private static RenterCollectionResponse toResponse(MoneyMovement moneyMovement) {
        return new RenterCollectionResponse(
                moneyMovement.getId(),
                moneyMovement.getPaymentPlan().getId(),
                moneyMovement.getType(),
                moneyMovement.getState(),
                moneyMovement.getAmount(),
                moneyMovement.getCurrency(),
                moneyMovement.getOperationKey(),
                moneyMovement.getCreatedAt()
        );
    }
}
