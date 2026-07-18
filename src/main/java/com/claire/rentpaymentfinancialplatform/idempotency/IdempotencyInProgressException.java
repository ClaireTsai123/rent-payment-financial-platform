package com.claire.rentpaymentfinancialplatform.idempotency;

public class IdempotencyInProgressException extends RuntimeException {

    public IdempotencyInProgressException(String idempotencyKey, String operation) {
        super("Idempotent operation is still in progress: " + operation + " / " + idempotencyKey);
    }
}
