package com.claire.rentpaymentfinancialplatform.operations;

import java.util.UUID;

public class OperationsResourceNotFoundException extends RuntimeException {

    public OperationsResourceNotFoundException(String resourceName, UUID id) {
        super(resourceName + " not found: " + id);
    }
}
