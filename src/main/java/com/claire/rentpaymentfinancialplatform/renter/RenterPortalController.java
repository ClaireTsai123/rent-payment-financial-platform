package com.claire.rentpaymentfinancialplatform.renter;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("hasRole('RENTER')")
public class RenterPortalController {

    private final RenterPortalQueryService renterPortalQueryService;

    public RenterPortalController(RenterPortalQueryService renterPortalQueryService) {
        this.renterPortalQueryService = renterPortalQueryService;
    }

    @GetMapping("/payment-plans")
    public Page<PaymentPlanSummaryResponse> listPaymentPlans(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return renterPortalQueryService.listPaymentPlans(pageable);
    }

    @GetMapping("/payment-plans/{paymentPlanId}")
    public PaymentPlanSummaryResponse getPaymentPlan(@PathVariable UUID paymentPlanId) {
        return renterPortalQueryService.getPaymentPlan(paymentPlanId);
    }

    @GetMapping("/money-movements")
    public Page<MoneyMovementSummaryResponse> listMoneyMovements(
            @RequestParam(required = false) UUID paymentPlanId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return renterPortalQueryService.listMoneyMovements(paymentPlanId, pageable);
    }

    @GetMapping("/money-movements/{moneyMovementId}")
    public MoneyMovementSummaryResponse getMoneyMovement(@PathVariable UUID moneyMovementId) {
        return renterPortalQueryService.getMoneyMovement(moneyMovementId);
    }
}
