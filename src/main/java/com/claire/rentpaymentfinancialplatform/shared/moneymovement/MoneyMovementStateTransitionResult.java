package com.claire.rentpaymentfinancialplatform.shared.moneymovement;

public record MoneyMovementStateTransitionResult(
        boolean applied,
        boolean noOp,
        String rejectionReason
) {

    public static MoneyMovementStateTransitionResult appliedTransition() {
        return new MoneyMovementStateTransitionResult(true, false, null);
    }

    public static MoneyMovementStateTransitionResult noOpTransition() {
        return new MoneyMovementStateTransitionResult(false, true, null);
    }

    public static MoneyMovementStateTransitionResult rejectedTransition(String rejectionReason) {
        return new MoneyMovementStateTransitionResult(false, false, rejectionReason);
    }
}
