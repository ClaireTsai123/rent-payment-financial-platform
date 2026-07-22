import type { PaymentPlanSummary } from "../../api/types";

export function isCollectionEligiblePaymentPlan(plan: PaymentPlanSummary) {
  return plan.status === "ACTIVE";
}
