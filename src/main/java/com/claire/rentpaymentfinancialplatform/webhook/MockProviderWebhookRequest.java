package com.claire.rentpaymentfinancialplatform.webhook;

import com.claire.rentpaymentfinancialplatform.shared.domain.ProviderTransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record MockProviderWebhookRequest(
        @NotBlank String providerEventId,
        @NotBlank String providerTransactionId,
        @NotNull ProviderTransactionStatus providerStatus,
        @NotNull Instant occurredAt
) {
}
