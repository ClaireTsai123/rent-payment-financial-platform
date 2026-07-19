package com.claire.rentpaymentfinancialplatform.provider;

import com.claire.rentpaymentfinancialplatform.shared.domain.MoneyMovementState;
import com.claire.rentpaymentfinancialplatform.shared.domain.PaymentAttemptStatus;
import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderTransactionStatus;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovement;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementStateHistory;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.MoneyMovementStateHistoryRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.PaymentAttempt;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.PaymentAttemptRepository;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransaction;
import com.claire.rentpaymentfinancialplatform.shared.moneymovement.ProviderTransactionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProviderSubmissionService {

    private static final int FIRST_ATTEMPT = 1;
    private static final String PROVIDER_SUBMITTED_REASON = "PROVIDER_SUBMITTED";
    private static final String PROVIDER_FAILED_REASON = "PROVIDER_FAILED";
    private static final String PROVIDER_AMBIGUOUS_REASON = "PROVIDER_AMBIGUOUS";

    private final PaymentProviderAdapter paymentProviderAdapter;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final ProviderTransactionRepository providerTransactionRepository;
    private final MoneyMovementStateHistoryRepository stateHistoryRepository;
    private final MoneyMovementRepository moneyMovementRepository;

    public ProviderSubmissionService(
            PaymentProviderAdapter paymentProviderAdapter,
            PaymentAttemptRepository paymentAttemptRepository,
            ProviderTransactionRepository providerTransactionRepository,
            MoneyMovementStateHistoryRepository stateHistoryRepository,
            MoneyMovementRepository moneyMovementRepository
    ) {
        this.paymentProviderAdapter = paymentProviderAdapter;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.providerTransactionRepository = providerTransactionRepository;
        this.stateHistoryRepository = stateHistoryRepository;
        this.moneyMovementRepository = moneyMovementRepository;
    }

    public void submit(MoneyMovement moneyMovement) {
        PaymentAttempt attempt = paymentAttemptRepository.saveAndFlush(new PaymentAttempt(
                UUID.randomUUID(),
                moneyMovement,
                FIRST_ATTEMPT,
                PaymentAttemptStatus.CREATED
        ));
        ProviderPaymentResponse providerResponse = paymentProviderAdapter.submit(toProviderRequest(moneyMovement));
        attempt.recordProviderResult(
                toAttemptStatus(providerResponse.normalizedStatus()),
                providerResponse.failureCode(),
                providerResponse.failureMessage()
        );
        paymentAttemptRepository.saveAndFlush(attempt);

        providerTransactionRepository.saveAndFlush(new ProviderTransaction(
                UUID.randomUUID(),
                moneyMovement,
                attempt,
                providerResponse.provider(),
                providerResponse.providerTransactionId(),
                providerResponse.providerIdempotencyKey(),
                providerResponse.normalizedStatus(),
                providerResponse.rawStatus()
        ));

        MoneyMovementState previousState = moneyMovement.getState();
        moneyMovement.transitionTo(toMoneyMovementState(providerResponse.normalizedStatus()));
        stateHistoryRepository.saveAndFlush(new MoneyMovementStateHistory(
                UUID.randomUUID(),
                moneyMovement,
                previousState,
                moneyMovement.getState(),
                toStateHistoryReason(providerResponse.normalizedStatus())
        ));
        moneyMovementRepository.saveAndFlush(moneyMovement);
    }

    private static ProviderPaymentRequest toProviderRequest(MoneyMovement moneyMovement) {
        return new ProviderPaymentRequest(
                moneyMovement.getId(),
                moneyMovement.getType(),
                moneyMovement.getAmount(),
                moneyMovement.getCurrency(),
                moneyMovement.getOperationKey(),
                providerIdempotencyKeyFor(moneyMovement)
        );
    }

    private static String providerIdempotencyKeyFor(MoneyMovement moneyMovement) {
        return moneyMovement.getOperationKey();
    }

    private static PaymentAttemptStatus toAttemptStatus(ProviderTransactionStatus providerStatus) {
        return switch (providerStatus) {
            case PROCESSING, PENDING -> PaymentAttemptStatus.PROCESSING;
            case SUCCEEDED -> PaymentAttemptStatus.SUCCEEDED;
            case FAILED, RETURNED, REVERSED -> PaymentAttemptStatus.FAILED;
            case UNKNOWN -> PaymentAttemptStatus.AMBIGUOUS;
        };
    }

    private static MoneyMovementState toMoneyMovementState(ProviderTransactionStatus providerStatus) {
        return switch (providerStatus) {
            case PROCESSING, PENDING, UNKNOWN -> MoneyMovementState.PROCESSING;
            case SUCCEEDED -> MoneyMovementState.SUCCEEDED;
            case FAILED -> MoneyMovementState.FAILED;
            case RETURNED -> MoneyMovementState.RETURNED;
            case REVERSED -> MoneyMovementState.REVERSED;
        };
    }

    private static String toStateHistoryReason(ProviderTransactionStatus providerStatus) {
        return switch (providerStatus) {
            case FAILED, RETURNED, REVERSED -> PROVIDER_FAILED_REASON;
            case UNKNOWN -> PROVIDER_AMBIGUOUS_REASON;
            case PENDING, PROCESSING, SUCCEEDED -> PROVIDER_SUBMITTED_REASON;
        };
    }
}
