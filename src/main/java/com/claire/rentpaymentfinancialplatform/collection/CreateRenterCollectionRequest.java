package com.claire.rentpaymentfinancialplatform.collection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateRenterCollectionRequest(
        @NotNull UUID paymentPlanId,
        @NotBlank @Size(max = 160) String operationKey,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency
) {
}
