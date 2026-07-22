package com.claire.rentpaymentfinancialplatform.renter;

import java.util.UUID;

public class MoneyMovementNotFoundException extends RuntimeException {

    public MoneyMovementNotFoundException(UUID moneyMovementId) {
        super("Money movement not found: " + moneyMovementId);
    }
}
