package com.claire.rentpaymentfinancialplatform.idempotency;

public class IdempotencyExpiredException extends RuntimeException {

    public IdempotencyExpiredException(String idempotencyKey, String operation) {
        super("Idempotency key has expired for operation: " + operation + " / " + idempotencyKey);
    }
}
