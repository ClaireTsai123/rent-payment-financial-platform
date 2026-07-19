package com.claire.rentpaymentfinancialplatform.provider;

import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderTransactionStatus;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentProviderAdapter implements PaymentProviderAdapter {

    static final String PROVIDER_NAME = "mock-provider";
    static final String DEFINITIVE_FAILURE_TRIGGER = "mock-fail";
    static final String AMBIGUOUS_TIMEOUT_TRIGGER = "mock-timeout";

    @Override
    public ProviderPaymentResponse submit(ProviderPaymentRequest request) {
        String deterministicReference = UUID.nameUUIDFromBytes(
                (request.moneyMovementType() + ":" + request.providerIdempotencyKey()).getBytes(StandardCharsets.UTF_8)
        ).toString();
        if (request.operationKey().contains(DEFINITIVE_FAILURE_TRIGGER)) {
            return new ProviderPaymentResponse(
                    PROVIDER_NAME,
                    "mock-failed-txn-" + deterministicReference,
                    request.providerIdempotencyKey(),
                    ProviderTransactionStatus.FAILED,
                    "DECLINED",
                    "MOCK_PROVIDER_DECLINED",
                    "Mock provider returned a definitive failure."
            );
        }
        if (request.operationKey().contains(AMBIGUOUS_TIMEOUT_TRIGGER)) {
            return new ProviderPaymentResponse(
                    PROVIDER_NAME,
                    "mock-timeout-interaction-" + deterministicReference,
                    request.providerIdempotencyKey(),
                    ProviderTransactionStatus.UNKNOWN,
                    "TIMEOUT",
                    "MOCK_PROVIDER_TIMEOUT",
                    "Mock provider timed out before final status was known."
            );
        }
        return new ProviderPaymentResponse(
                PROVIDER_NAME,
                "mock-txn-" + deterministicReference,
                request.providerIdempotencyKey(),
                ProviderTransactionStatus.PROCESSING,
                "PROCESSING",
                null,
                null
        );
    }
}
