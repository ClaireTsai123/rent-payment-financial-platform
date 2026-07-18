package com.claire.rentpaymentfinancialplatform.disbursement;

import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlan;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlanRepository;
import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyOperation;
import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyRecord;
import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyService;
import com.claire.rentpaymentfinancialplatform.shared.api.PaymentPlanNotFoundException;
import com.claire.rentpaymentfinancialplatform.shared.domain.IdempotencyStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovement;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyDisbursementService {

    private final PaymentPlanRepository paymentPlanRepository;
    private final MoneyMovementRepository moneyMovementRepository;
    private final IdempotencyService idempotencyService;

    public PropertyDisbursementService(
            PaymentPlanRepository paymentPlanRepository,
            MoneyMovementRepository moneyMovementRepository,
            IdempotencyService idempotencyService
    ) {
        this.paymentPlanRepository = paymentPlanRepository;
        this.moneyMovementRepository = moneyMovementRepository;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public PropertyDisbursementResponse createDisbursement(String idempotencyKey, CreatePropertyDisbursementRequest request) {
        IdempotencyRecord idempotencyRecord = idempotencyService.startOrReplay(
                idempotencyKey,
                IdempotencyOperation.PROPERTY_DISBURSEMENT,
                request
        );
        if (idempotencyRecord.getStatus() == IdempotencyStatus.COMPLETED) {
            return idempotencyService.readStoredResponse(idempotencyRecord, PropertyDisbursementResponse.class);
        }

        PaymentPlan paymentPlan = paymentPlanRepository.findById(request.paymentPlanId())
                .orElseThrow(() -> new PaymentPlanNotFoundException(request.paymentPlanId()));

        MoneyMovement moneyMovement = moneyMovementRepository.saveAndFlush(new MoneyMovement(
                UUID.randomUUID(),
                paymentPlan,
                MoneyMovementType.PROPERTY_DISBURSEMENT,
                MoneyMovementState.CREATED,
                paymentPlan.getRentAmount(),
                request.currency(),
                request.operationKey()
        ));

        PropertyDisbursementResponse response = toResponse(moneyMovement);
        idempotencyService.complete(idempotencyRecord, moneyMovement.getId(), response);
        return response;
    }

    private static PropertyDisbursementResponse toResponse(MoneyMovement moneyMovement) {
        return new PropertyDisbursementResponse(
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
