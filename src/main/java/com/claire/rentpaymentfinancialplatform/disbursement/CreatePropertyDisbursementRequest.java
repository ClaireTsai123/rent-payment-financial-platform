package com.claire.rentpaymentfinancialplatform.disbursement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreatePropertyDisbursementRequest(
        @NotNull UUID paymentPlanId,
        @NotBlank @Size(max = 160) String operationKey,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency
) {

    public CreatePropertyDisbursementRequest {
        operationKey = operationKey == null ? null : operationKey.trim();
        currency = currency == null ? null : currency.trim().toUpperCase();
    }
}
