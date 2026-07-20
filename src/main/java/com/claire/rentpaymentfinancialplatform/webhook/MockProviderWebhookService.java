package com.claire.rentpaymentfinancialplatform.webhook;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MockProviderWebhookService {

    private static final String PROVIDER = "mock-provider";
    private static final String APPLIED = "APPLIED";
    private static final String DUPLICATE = "DUPLICATE";
    private static final String UNMATCHED = "UNMATCHED";
    private static final String IGNORED = "IGNORED";

    private final String sharedSecret;
    private final ProviderWebhookEventRepository webhookEventRepository;
    private final ProviderTransactionRepository providerTransactionRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final MoneyMovementRepository moneyMovementRepository;
    private final MoneyMovementStateHistoryRepository stateHistoryRepository;

    public MockProviderWebhookService(
            @Value("${provider.webhook.mock-provider.shared-secret:local-mock-webhook-secret}") String sharedSecret,
            ProviderWebhookEventRepository webhookEventRepository,
            ProviderTransactionRepository providerTransactionRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            MoneyMovementRepository moneyMovementRepository,
            MoneyMovementStateHistoryRepository stateHistoryRepository
    ) {
        this.sharedSecret = sharedSecret;
        this.webhookEventRepository = webhookEventRepository;
        this.providerTransactionRepository = providerTransactionRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.moneyMovementRepository = moneyMovementRepository;
        this.stateHistoryRepository = stateHistoryRepository;
    }

    @Transactional
    public ProviderWebhookResponse receive(String signature, MockProviderWebhookRequest request, String rawPayload) {
        if (!sharedSecret.equals(signature)) {
            throw new WebhookSignatureException();
        }
        if (webhookEventRepository.findByProviderAndProviderEventId(PROVIDER, request.providerEventId()).isPresent()) {
            return new ProviderWebhookResponse(DUPLICATE);
        }

        ProviderWebhookEvent event = webhookEventRepository.saveAndFlush(new ProviderWebhookEvent(
                UUID.randomUUID(),
                PROVIDER,
                request.providerEventId(),
                request.providerTransactionId(),
                request.providerStatus(),
                rawPayload,
                request.occurredAt()
        ));

        return providerTransactionRepository.findByProviderAndProviderTransactionId(PROVIDER, request.providerTransactionId())
                .map(providerTransaction -> apply(event, providerTransaction))
                .orElseGet(() -> unmatched(event));
    }

    private ProviderWebhookResponse apply(ProviderWebhookEvent event, ProviderTransaction providerTransaction) {
        if (!canApply(providerTransaction.getNormalizedStatus(), event.getNormalizedStatus())) {
            event.ignored("Webhook status would regress a terminal provider transaction.");
            return new ProviderWebhookResponse(IGNORED);
        }

        MoneyMovement moneyMovement = providerTransaction.getMoneyMovement();
        PaymentAttempt paymentAttempt = providerTransaction.getPaymentAttempt();
        MoneyMovementState previousState = moneyMovement.getState();
        MoneyMovementState nextState = toMoneyMovementState(event.getNormalizedStatus());

        providerTransaction.recordProviderStatus(event.getNormalizedStatus(), event.getNormalizedStatus().name());
        paymentAttempt.recordProviderResult(
                toAttemptStatus(event.getNormalizedStatus()),
                failureCodeFor(event.getNormalizedStatus()),
                failureMessageFor(event.getNormalizedStatus())
        );
        moneyMovement.transitionTo(nextState);

        providerTransactionRepository.saveAndFlush(providerTransaction);
        paymentAttemptRepository.saveAndFlush(paymentAttempt);
        moneyMovementRepository.saveAndFlush(moneyMovement);
        if (previousState != nextState) {
            stateHistoryRepository.saveAndFlush(new MoneyMovementStateHistory(
                    UUID.randomUUID(),
                    moneyMovement,
                    previousState,
                    nextState,
                    reasonFor(event.getNormalizedStatus())
            ));
        }
        event.applied();
        return new ProviderWebhookResponse(APPLIED);
    }

    private ProviderWebhookResponse unmatched(ProviderWebhookEvent event) {
        event.unmatched("No provider transaction matched this webhook.");
        return new ProviderWebhookResponse(UNMATCHED);
    }

    private static boolean canApply(ProviderTransactionStatus currentStatus, ProviderTransactionStatus nextStatus) {
        if (!isTerminal(currentStatus)) {
            return true;
        }
        return currentStatus == nextStatus;
    }

    private static boolean isTerminal(ProviderTransactionStatus status) {
        return status == ProviderTransactionStatus.SUCCEEDED
                || status == ProviderTransactionStatus.FAILED
                || status == ProviderTransactionStatus.RETURNED
                || status == ProviderTransactionStatus.REVERSED;
    }

    private static PaymentAttemptStatus toAttemptStatus(ProviderTransactionStatus providerStatus) {
        return switch (providerStatus) {
            case PENDING, PROCESSING -> PaymentAttemptStatus.PROCESSING;
            case SUCCEEDED -> PaymentAttemptStatus.SUCCEEDED;
            case FAILED, RETURNED, REVERSED -> PaymentAttemptStatus.FAILED;
            case UNKNOWN -> PaymentAttemptStatus.AMBIGUOUS;
        };
    }

    private static MoneyMovementState toMoneyMovementState(ProviderTransactionStatus providerStatus) {
        return switch (providerStatus) {
            case PENDING, PROCESSING, UNKNOWN -> MoneyMovementState.PROCESSING;
            case SUCCEEDED -> MoneyMovementState.SUCCEEDED;
            case FAILED -> MoneyMovementState.FAILED;
            case RETURNED -> MoneyMovementState.RETURNED;
            case REVERSED -> MoneyMovementState.REVERSED;
        };
    }

    private static String reasonFor(ProviderTransactionStatus providerStatus) {
        return switch (providerStatus) {
            case SUCCEEDED -> "WEBHOOK_SUCCEEDED";
            case FAILED, RETURNED, REVERSED -> "WEBHOOK_FAILED";
            case UNKNOWN -> "WEBHOOK_AMBIGUOUS";
            case PENDING, PROCESSING -> "WEBHOOK_PROCESSING";
        };
    }

    private static String failureCodeFor(ProviderTransactionStatus providerStatus) {
        return switch (providerStatus) {
            case FAILED, RETURNED, REVERSED -> "PROVIDER_WEBHOOK_" + providerStatus.name();
            case UNKNOWN -> "PROVIDER_WEBHOOK_UNKNOWN";
            case PENDING, PROCESSING, SUCCEEDED -> null;
        };
    }

    private static String failureMessageFor(ProviderTransactionStatus providerStatus) {
        return switch (providerStatus) {
            case FAILED, RETURNED, REVERSED -> "Provider webhook reported " + providerStatus.name() + ".";
            case UNKNOWN -> "Provider webhook did not provide a final status.";
            case PENDING, PROCESSING, SUCCEEDED -> null;
        };
    }
}
