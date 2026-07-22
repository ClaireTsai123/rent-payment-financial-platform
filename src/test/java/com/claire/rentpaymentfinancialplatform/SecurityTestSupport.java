package com.claire.rentpaymentfinancialplatform;

public final class SecurityTestSupport {

    private SecurityTestSupport() {
    }

    public static String renterToken(String renterId) {
        return "Bearer dev:test-user:" + renterId + ":RENTER";
    }

    public static String finopsToken() {
        return "Bearer dev:finops-user:-:FINOPS";
    }
}
