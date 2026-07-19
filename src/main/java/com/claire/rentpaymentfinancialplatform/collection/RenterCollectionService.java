package com.claire.rentpaymentfinancialplatform.collection;

import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlan;
import com.claire.rentpaymentfinancialplatform.paymentplan.PaymentPlanRepository;
import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyOperation;
import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyRecord;
import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyService;
import com.claire.rentpaymentfinancialplatform.provider.ProviderSubmissionService;
import com.claire.rentpaymentfinancialplatform.shared.domain.IdempotencyStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementType;
import com.claire.rentpaymentfinancialplatform.shared.api.PaymentPlanNotFoundException;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovement;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RenterCollectionService {

    private final PaymentPlanRepository paymentPlanRepository;
    private final MoneyMovementRepository moneyMovementRepository;
    private final IdempotencyService idempotencyService;
    private final ProviderSubmissionService providerSubmissionService;

    public RenterCollectionService(
            PaymentPlanRepository paymentPlanRepository,
            MoneyMovementRepository moneyMovementRepository,
            IdempotencyService idempotencyService,
            ProviderSubmissionService providerSubmissionService
    ) {
        this.paymentPlanRepository = paymentPlanRepository;
        this.moneyMovementRepository = moneyMovementRepository;
        this.idempotencyService = idempotencyService;
        this.providerSubmissionService = providerSubmissionService;
    }

    @Transactional
    public RenterCollectionResponse createCollection(String idempotencyKey, CreateRenterCollectionRequest request) {
        IdempotencyRecord idempotencyRecord = idempotencyService.startOrReplay(
                idempotencyKey,
                IdempotencyOperation.RENTER_COLLECTION,
                request
        );
        if (idempotencyRecord.getStatus() == IdempotencyStatus.COMPLETED) {
            return idempotencyService.readStoredResponse(idempotencyRecord, RenterCollectionResponse.class);
        }

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
        providerSubmissionService.submit(moneyMovement);

        RenterCollectionResponse response = toResponse(moneyMovement);
        idempotencyService.complete(idempotencyRecord, moneyMovement.getId(), response);
        return response;
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
