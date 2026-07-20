package com.claire.rentpaymentfinancialplatform.shared.api;

import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyConflictException;
import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyExpiredException;
import com.claire.rentpaymentfinancialplatform.idempotency.IdempotencyInProgressException;
import com.claire.rentpaymentfinancialplatform.webhook.WebhookSignatureException;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(PaymentPlanNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiErrorResponse handlePaymentPlanNotFound(PaymentPlanNotFoundException exception) {
        return new ApiErrorResponse("PAYMENT_PLAN_NOT_FOUND", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        return new ApiErrorResponse("DATA_INTEGRITY_VIOLATION", "Request conflicts with existing payment data.", Instant.now());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiErrorResponse handleIdempotencyConflict(IdempotencyConflictException exception) {
        return new ApiErrorResponse("IDEMPOTENCY_KEY_CONFLICT", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(IdempotencyInProgressException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiErrorResponse handleIdempotencyInProgress(IdempotencyInProgressException exception) {
        return new ApiErrorResponse("IDEMPOTENCY_IN_PROGRESS", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(IdempotencyExpiredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiErrorResponse handleIdempotencyExpired(IdempotencyExpiredException exception) {
        return new ApiErrorResponse("IDEMPOTENCY_KEY_EXPIRED", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiErrorResponse handleMissingRequestHeader(MissingRequestHeaderException exception) {
        return new ApiErrorResponse("MISSING_REQUEST_HEADER", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiErrorResponse handleValidationFailure(MethodArgumentNotValidException exception) {
        return new ApiErrorResponse("VALIDATION_FAILED", "Request validation failed.", Instant.now());
    }

    @ExceptionHandler(WebhookSignatureException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ApiErrorResponse handleWebhookSignatureFailure(WebhookSignatureException exception) {
        return new ApiErrorResponse("WEBHOOK_SIGNATURE_INVALID", exception.getMessage(), Instant.now());
    }
}
