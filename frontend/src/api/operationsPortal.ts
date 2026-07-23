import { apiRequest } from "./http";
import type {
  MoneyMovementSummary,
  OperationsMoneyMovementDetail,
  OutboxEvent,
  PageResponse,
  ProviderTransaction,
  ProviderWebhookEvent,
  ReconciliationException,
  ReconciliationRun,
  SettlementRecord
} from "./types";

export const OPERATIONS_PAGE_SIZE = 20;

export type OperationsResourceKey =
  | "money-movements"
  | "provider-transactions"
  | "provider-webhook-events"
  | "outbox-events"
  | "settlement-records"
  | "reconciliation-runs"
  | "reconciliation-exceptions";

export type OperationsResourceMap = {
  "money-movements": MoneyMovementSummary;
  "provider-transactions": ProviderTransaction;
  "provider-webhook-events": ProviderWebhookEvent;
  "outbox-events": OutboxEvent;
  "settlement-records": SettlementRecord;
  "reconciliation-runs": ReconciliationRun;
  "reconciliation-exceptions": ReconciliationException;
};

export type OperationsDetailMap = OperationsResourceMap & {
  "money-movements": OperationsMoneyMovementDetail;
};

export type OperationsFilters = Record<string, string>;

export function listOperationsResource<K extends OperationsResourceKey>(
  token: string,
  resource: K,
  filters: OperationsFilters,
  page = 0,
  size = OPERATIONS_PAGE_SIZE
) {
  const searchParams = new URLSearchParams({
    page: String(page),
    size: String(size)
  });
  Object.entries(filters).forEach(([key, value]) => {
    const normalizedValue = normalizeFilterValue(value);
    if (normalizedValue) {
      searchParams.set(key, normalizedValue);
    }
  });

  return apiRequest<PageResponse<OperationsResourceMap[K]>>(`/api/v1/ops/${resource}?${searchParams}`, { token });
}

export function getOperationsResource<K extends OperationsResourceKey>(token: string, resource: K, id: string) {
  return apiRequest<OperationsDetailMap[K]>(`/api/v1/ops/${resource}/${id}`, { token });
}

function normalizeFilterValue(value: string) {
  const trimmed = value.trim();
  if (!trimmed) {
    return "";
  }
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(trimmed)) {
    return new Date(trimmed).toISOString();
  }
  return trimmed;
}
