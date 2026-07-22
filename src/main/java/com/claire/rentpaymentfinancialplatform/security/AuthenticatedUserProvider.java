package com.claire.rentpaymentfinancialplatform.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

    public ApplicationUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ApplicationUser user)) {
            throw new IllegalStateException("Authenticated application user is required.");
        }
        return user;
    }

    public String currentRenterId() {
        String renterId = currentUser().renterId();
        if (renterId == null || renterId.isBlank()) {
            throw new IllegalStateException("Authenticated renter identity is required.");
        }
        return renterId;
    }
}
