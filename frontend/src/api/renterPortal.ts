import { apiRequest } from "./http";
import type {
  CreateRenterCollectionRequest,
  MoneyMovementSummary,
  PageResponse,
  PaymentPlanSummary,
  RenterCollectionResponse
} from "./types";

const DEFAULT_PAGE_SIZE = 20;

export function listPaymentPlans(token: string, page = 0, size = DEFAULT_PAGE_SIZE) {
  return apiRequest<PageResponse<PaymentPlanSummary>>(
    `/api/v1/me/payment-plans?page=${page}&size=${size}&sort=createdAt,desc`,
    { token }
  );
}

export function getPaymentPlan(token: string, paymentPlanId: string) {
  return apiRequest<PaymentPlanSummary>(`/api/v1/me/payment-plans/${paymentPlanId}`, { token });
}

export function listMoneyMovements(token: string, paymentPlanId?: string, page = 0, size = DEFAULT_PAGE_SIZE) {
  const searchParams = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: "createdAt,desc"
  });
  if (paymentPlanId) {
    searchParams.set("paymentPlanId", paymentPlanId);
  }

  return apiRequest<PageResponse<MoneyMovementSummary>>(`/api/v1/me/money-movements?${searchParams}`, { token });
}

export function getMoneyMovement(token: string, moneyMovementId: string) {
  return apiRequest<MoneyMovementSummary>(`/api/v1/me/money-movements/${moneyMovementId}`, { token });
}

export function createRenterCollection(
  token: string,
  idempotencyKey: string,
  request: CreateRenterCollectionRequest
) {
  return apiRequest<RenterCollectionResponse>("/api/v1/renter-collections", {
    token,
    idempotencyKey,
    method: "POST",
    body: request
  });
}
