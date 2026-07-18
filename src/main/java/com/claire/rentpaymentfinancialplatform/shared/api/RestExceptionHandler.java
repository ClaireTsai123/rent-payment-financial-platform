package com.claire.rentpaymentfinancialplatform.shared.api;

import com.claire.rentpaymentfinancialplatform.collection.PaymentPlanNotFoundException;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiErrorResponse handleValidationFailure(MethodArgumentNotValidException exception) {
        return new ApiErrorResponse("VALIDATION_FAILED", "Request validation failed.", Instant.now());
    }
}
