package com.claire.rentpaymentfinancialplatform.collection;

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
@RequestMapping("/api/v1/renter-collections")
@PreAuthorize("hasRole('RENTER')")
public class RenterCollectionController {

    private final RenterCollectionService renterCollectionService;

    public RenterCollectionController(RenterCollectionService renterCollectionService) {
        this.renterCollectionService = renterCollectionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RenterCollectionResponse createCollection(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateRenterCollectionRequest request
    ) {
        return renterCollectionService.createCollection(idempotencyKey, request);
    }
}
