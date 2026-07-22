export type PaymentPlanStatus = "CREATED" | "ACTIVE" | "COMPLETED" | "CANCELLED";
export type MoneyMovementType = "RENTER_COLLECTION" | "PROPERTY_DISBURSEMENT";
export type MoneyMovementState = "CREATED" | "SUBMITTED" | "PROCESSING" | "SUCCEEDED" | "FAILED" | "RETURNED" | "REVERSED";
export type ProviderTransactionStatus = "PENDING" | "PROCESSING" | "SUCCEEDED" | "FAILED" | "RETURNED" | "REVERSED" | "UNKNOWN";
export type ProviderWebhookEventStatus = "RECEIVED" | "APPLIED" | "DUPLICATE" | "UNMATCHED" | "IGNORED";
export type OutboxEventStatus = "PENDING" | "PUBLISHED" | "FAILED";
export type SettlementStatus = "EXPECTED" | "SETTLED" | "MISMATCHED";
export type ReconciliationRunStatus = "STARTED" | "COMPLETED" | "FAILED";
export type ReconciliationExceptionType = "MISSING_SETTLEMENT" | "AMOUNT_MISMATCH" | "DUPLICATE_PROVIDER_RECORD";

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
};

export type PaymentPlanSummary = {
  id: string;
  renterId: string;
  billingObligationId: string;
  rentAmount: number;
  initialCollectionAmount: number;
  repaymentAmount: number;
  rentDueDate: string;
  repaymentDueDate: string;
  status: PaymentPlanStatus;
  createdAt: string;
  updatedAt: string;
};

export type MoneyMovementSummary = {
  id: string;
  paymentPlanId: string;
  renterId?: string;
  type: MoneyMovementType;
  state: MoneyMovementState;
  amount: number;
  currency: string;
  operationKey: string;
  createdAt: string;
  updatedAt: string;
};

export type MoneyMovementStateHistoryItem = {
  id: string;
  fromState: MoneyMovementState | null;
  toState: MoneyMovementState;
  reason: string;
  changedAt: string;
};

export type OperationsMoneyMovementDetail = MoneyMovementSummary & {
  renterId: string;
  version: number;
  stateHistory: MoneyMovementStateHistoryItem[];
};

export type ProviderTransaction = {
  id: string;
  moneyMovementId: string;
  paymentAttemptId: string;
  provider: string;
  providerTransactionId: string;
  providerIdempotencyKey: string | null;
  normalizedStatus: ProviderTransactionStatus;
  rawStatus: string | null;
  settlementReference: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ProviderWebhookEvent = {
  id: string;
  provider: string;
  providerEventId: string;
  providerTransactionId: string;
  normalizedStatus: ProviderTransactionStatus;
  rawPayload: string;
  processingStatus: ProviderWebhookEventStatus;
  failureReason: string | null;
  occurredAt: string;
  receivedAt: string;
  processedAt: string | null;
};

export type OutboxEvent = {
  id: string;
  aggregateType: string;
  aggregateId: string;
  eventType: string;
  payload: string;
  status: OutboxEventStatus;
  attempts: number;
  lastError: string | null;
  publishedAt: string | null;
  nextAttemptAt: string;
  version: number;
  createdAt: string;
  updatedAt: string;
};

export type SettlementRecord = {
  id: string;
  moneyMovementId: string;
  providerTransactionId: string;
  status: SettlementStatus;
  expectedGrossAmount: number;
  expectedFeeAmount: number;
  expectedNetAmount: number;
  actualGrossAmount: number | null;
  actualFeeAmount: number | null;
  actualNetAmount: number | null;
  currency: string;
  expectedSettlementDate: string;
  actualSettlementDate: string | null;
  provider: string;
  providerTransactionReference: string;
  providerBatchReference: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ReconciliationRun = {
  id: string;
  sourceFile: string;
  status: ReconciliationRunStatus;
  totalRows: number;
  matchedRows: number;
  exceptionRows: number;
  startedAt: string;
  completedAt: string | null;
  failureReason: string | null;
};

export type ReconciliationException = {
  id: string;
  reconciliationRunId: string;
  exceptionType: ReconciliationExceptionType;
  provider: string;
  providerTransactionReference: string;
  message: string;
  rawRecord: string;
  createdAt: string;
};

export type CreateRenterCollectionRequest = {
  paymentPlanId: string;
  operationKey: string;
  currency: string;
};

export type RenterCollectionResponse = {
  moneyMovementId: string;
  paymentPlanId: string;
  type: MoneyMovementType;
  state: MoneyMovementState;
  amount: number;
  currency: string;
  operationKey: string;
  createdAt: string;
};

export type ApiErrorResponse = {
  code?: string;
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
};
