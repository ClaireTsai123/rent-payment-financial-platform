package com.claire.rentpaymentfinancialplatform.disbursement;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/property-disbursements")
@PreAuthorize("hasAnyRole('FINOPS', 'ADMIN')")
public class PropertyDisbursementController {

    private final PropertyDisbursementService propertyDisbursementService;

    public PropertyDisbursementController(PropertyDisbursementService propertyDisbursementService) {
        this.propertyDisbursementService = propertyDisbursementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyDisbursementResponse createDisbursement(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePropertyDisbursementRequest request
    ) {
        return propertyDisbursementService.createDisbursement(idempotencyKey, request);
    }
}
