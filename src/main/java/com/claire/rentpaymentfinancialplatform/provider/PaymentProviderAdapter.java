package com.claire.rentpaymentfinancialplatform.provider;

public interface PaymentProviderAdapter {

    ProviderPaymentResponse submit(ProviderPaymentRequest request);
}
