package com.claire.rentpaymentfinancialplatform.renter;

import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlan;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlanRepository;
import com.claire.rentpaymentfinancialplatform.security.AuthenticatedUserProvider;
import com.claire.rentpaymentfinancialplatform.shared.api.PaymentPlanNotFoundException;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovement;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RenterPortalQueryService {

    private final PaymentPlanRepository paymentPlanRepository;
    private final MoneyMovementRepository moneyMovementRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public RenterPortalQueryService(
            PaymentPlanRepository paymentPlanRepository,
            MoneyMovementRepository moneyMovementRepository,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.paymentPlanRepository = paymentPlanRepository;
        this.moneyMovementRepository = moneyMovementRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Transactional(readOnly = true)
    public Page<PaymentPlanSummaryResponse> listPaymentPlans(Pageable pageable) {
        return paymentPlanRepository.findByRenterId(authenticatedUserProvider.currentRenterId(), pageable)
                .map(RenterPortalQueryService::toPaymentPlanResponse);
    }

    @Transactional(readOnly = true)
    public PaymentPlanSummaryResponse getPaymentPlan(UUID paymentPlanId) {
        String renterId = authenticatedUserProvider.currentRenterId();
        return paymentPlanRepository.findByIdAndRenterId(paymentPlanId, renterId)
                .map(RenterPortalQueryService::toPaymentPlanResponse)
                .orElseThrow(() -> new PaymentPlanNotFoundException(paymentPlanId));
    }

    @Transactional(readOnly = true)
    public Page<MoneyMovementSummaryResponse> listMoneyMovements(UUID paymentPlanId, Pageable pageable) {
        String renterId = authenticatedUserProvider.currentRenterId();
        Page<MoneyMovement> movements = paymentPlanId == null
                ? moneyMovementRepository.findByPaymentPlanRenterId(renterId, pageable)
                : moneyMovementRepository.findByPaymentPlanRenterIdAndPaymentPlanId(renterId, paymentPlanId, pageable);
        return movements.map(RenterPortalQueryService::toMoneyMovementResponse);
    }

    @Transactional(readOnly = true)
    public MoneyMovementSummaryResponse getMoneyMovement(UUID moneyMovementId) {
        String renterId = authenticatedUserProvider.currentRenterId();
        return moneyMovementRepository.findByIdAndPaymentPlanRenterId(moneyMovementId, renterId)
                .map(RenterPortalQueryService::toMoneyMovementResponse)
                .orElseThrow(() -> new MoneyMovementNotFoundException(moneyMovementId));
    }

    private static PaymentPlanSummaryResponse toPaymentPlanResponse(PaymentPlan paymentPlan) {
        return new PaymentPlanSummaryResponse(
                paymentPlan.getId(),
                paymentPlan.getRenterId(),
                paymentPlan.getBillingObligationId(),
                paymentPlan.getRentAmount(),
                paymentPlan.getInitialCollectionAmount(),
                paymentPlan.getRepaymentAmount(),
                paymentPlan.getRentDueDate(),
                paymentPlan.getRepaymentDueDate(),
                paymentPlan.getStatus(),
                paymentPlan.getCreatedAt(),
                paymentPlan.getUpdatedAt()
        );
    }

    private static MoneyMovementSummaryResponse toMoneyMovementResponse(MoneyMovement moneyMovement) {
        return new MoneyMovementSummaryResponse(
                moneyMovement.getId(),
                moneyMovement.getPaymentPlan().getId(),
                moneyMovement.getType(),
                moneyMovement.getState(),
                moneyMovement.getAmount(),
                moneyMovement.getCurrency(),
                moneyMovement.getOperationKey(),
                moneyMovement.getCreatedAt(),
                moneyMovement.getUpdatedAt()
        );
    }
}
