package com.claire.rentpaymentfinancialplatform.webhook;

public class WebhookSignatureException extends RuntimeException {

    public WebhookSignatureException() {
        super("Webhook signature is invalid.");
    }
}
