export type PaymentPlanStatus = "CREATED" | "ACTIVE" | "COMPLETED" | "CANCELLED";
export type MoneyMovementType = "RENTER_COLLECTION" | "PROPERTY_DISBURSEMENT";
export type MoneyMovementState = "CREATED" | "SUBMITTED" | "PROCESSING" | "SUCCEEDED" | "FAILED" | "RETURNED" | "REVERSED";

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
  type: MoneyMovementType;
  state: MoneyMovementState;
  amount: number;
  currency: string;
  operationKey: string;
  createdAt: string;
  updatedAt: string;
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
