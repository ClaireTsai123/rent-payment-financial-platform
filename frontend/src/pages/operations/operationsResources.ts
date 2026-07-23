import type { OperationsResourceKey } from "../../api/operationsPortal";

export type FilterOption = {
  name: string;
  label: string;
  type?: "text" | "datetime-local" | "date" | "select";
  options?: string[];
  exact?: boolean;
};

export type RelatedLinkConfig = {
  field: string;
  label: string;
  resourceKey: OperationsResourceKey;
  filter?: string;
  detail?: boolean;
  when?: (record: Record<string, unknown>) => boolean;
};

export type OperationsResourceConfig = {
  key: OperationsResourceKey;
  label: string;
  navLabel: string;
  detailLabel: string;
  statusField?: string;
  columns: { key: string; label: string; kind?: "status" | "date" | "money" | "code" }[];
  filters: FilterOption[];
  detailFields: { key: string; label: string; kind?: "status" | "date" | "money" | "code" | "multiline" }[];
  relatedLinks?: RelatedLinkConfig[];
};

const lifecycleFilters: FilterOption[] = [
  { name: "createdFrom", label: "Created from", type: "datetime-local" },
  { name: "createdTo", label: "Created to", type: "datetime-local" }
];

export const operationsResources: OperationsResourceConfig[] = [
  {
    key: "money-movements",
    label: "Money movements",
    navLabel: "Money",
    detailLabel: "Money movement",
    statusField: "state",
    columns: [
      { key: "operationKey", label: "Operation key", kind: "code" },
      { key: "type", label: "Type" },
      { key: "state", label: "State", kind: "status" },
      { key: "amount", label: "Amount", kind: "money" },
      { key: "createdAt", label: "Created", kind: "date" }
    ],
    filters: [
      { name: "id", label: "Movement ID", exact: true },
      { name: "paymentPlanId", label: "Payment plan ID", exact: true },
      { name: "renterId", label: "Renter ID", exact: true },
      { name: "state", label: "State", type: "select", options: ["CREATED", "SUBMITTED", "PROCESSING", "SUCCEEDED", "FAILED", "RETURNED", "REVERSED"] },
      { name: "type", label: "Type", type: "select", options: ["RENTER_COLLECTION", "PROPERTY_DISBURSEMENT"] },
      { name: "operationKey", label: "Operation key", exact: true },
      ...lifecycleFilters
    ],
    detailFields: [
      { key: "id", label: "ID", kind: "code" },
      { key: "paymentPlanId", label: "Payment plan", kind: "code" },
      { key: "renterId", label: "Renter" },
      { key: "type", label: "Type" },
      { key: "state", label: "State", kind: "status" },
      { key: "amount", label: "Amount", kind: "money" },
      { key: "operationKey", label: "Operation key", kind: "code" },
      { key: "createdAt", label: "Created", kind: "date" },
      { key: "updatedAt", label: "Updated", kind: "date" }
    ],
    relatedLinks: [
      { field: "id", label: "Provider transactions for this movement", resourceKey: "provider-transactions", filter: "moneyMovementId" },
      { field: "id", label: "Outbox events for this movement", resourceKey: "outbox-events", filter: "aggregateId" },
      { field: "id", label: "Settlement records for this movement", resourceKey: "settlement-records", filter: "moneyMovementId" }
    ]
  },
  {
    key: "provider-transactions",
    label: "Provider transactions",
    navLabel: "Provider Txns",
    detailLabel: "Provider transaction",
    statusField: "normalizedStatus",
    columns: [
      { key: "providerTransactionId", label: "Provider ref", kind: "code" },
      { key: "provider", label: "Provider" },
      { key: "normalizedStatus", label: "Status", kind: "status" },
      { key: "moneyMovementId", label: "Movement", kind: "code" },
      { key: "createdAt", label: "Created", kind: "date" }
    ],
    filters: [
      { name: "id", label: "Transaction ID", exact: true },
      { name: "moneyMovementId", label: "Movement ID", exact: true },
      { name: "paymentAttemptId", label: "Attempt ID", exact: true },
      { name: "provider", label: "Provider" },
      { name: "status", label: "Status", type: "select", options: ["PENDING", "PROCESSING", "SUCCEEDED", "FAILED", "RETURNED", "REVERSED", "UNKNOWN"] },
      { name: "providerTransactionId", label: "Provider ref", exact: true },
      { name: "providerIdempotencyKey", label: "Provider idem key", exact: true },
      ...lifecycleFilters
    ],
    detailFields: [
      { key: "id", label: "ID", kind: "code" },
      { key: "moneyMovementId", label: "Movement", kind: "code" },
      { key: "paymentAttemptId", label: "Attempt", kind: "code" },
      { key: "provider", label: "Provider" },
      { key: "providerTransactionId", label: "Provider ref", kind: "code" },
      { key: "providerIdempotencyKey", label: "Provider idem key", kind: "code" },
      { key: "normalizedStatus", label: "Status", kind: "status" },
      { key: "rawStatus", label: "Raw status" },
      { key: "settlementReference", label: "Settlement ref", kind: "code" },
      { key: "createdAt", label: "Created", kind: "date" },
      { key: "updatedAt", label: "Updated", kind: "date" }
    ],
    relatedLinks: [
      { field: "moneyMovementId", label: "Open money movement", resourceKey: "money-movements", detail: true },
      { field: "providerTransactionId", label: "Webhook events for this provider ref", resourceKey: "provider-webhook-events", filter: "providerTransactionId" },
      { field: "providerTransactionId", label: "Settlement records for this provider ref", resourceKey: "settlement-records", filter: "providerTransactionReference" },
      { field: "providerTransactionId", label: "Reconciliation exceptions for this provider ref", resourceKey: "reconciliation-exceptions", filter: "providerTransactionReference" }
    ]
  },
  {
    key: "provider-webhook-events",
    label: "Provider webhook events",
    navLabel: "Webhooks",
    detailLabel: "Provider webhook event",
    statusField: "processingStatus",
    columns: [
      { key: "providerEventId", label: "Event", kind: "code" },
      { key: "providerTransactionId", label: "Provider ref", kind: "code" },
      { key: "processingStatus", label: "Processing", kind: "status" },
      { key: "normalizedStatus", label: "Provider status", kind: "status" },
      { key: "receivedAt", label: "Received", kind: "date" }
    ],
    filters: [
      { name: "id", label: "Event ID", exact: true },
      { name: "provider", label: "Provider" },
      { name: "providerEventId", label: "Provider event ID", exact: true },
      { name: "providerTransactionId", label: "Provider ref", exact: true },
      { name: "normalizedStatus", label: "Provider status", type: "select", options: ["PENDING", "PROCESSING", "SUCCEEDED", "FAILED", "RETURNED", "REVERSED", "UNKNOWN"] },
      { name: "status", label: "Processing", type: "select", options: ["RECEIVED", "APPLIED", "DUPLICATE", "UNMATCHED", "IGNORED"] },
      { name: "receivedFrom", label: "Received from", type: "datetime-local" },
      { name: "receivedTo", label: "Received to", type: "datetime-local" }
    ],
    detailFields: [
      { key: "id", label: "ID", kind: "code" },
      { key: "provider", label: "Provider" },
      { key: "providerEventId", label: "Provider event", kind: "code" },
      { key: "providerTransactionId", label: "Provider ref", kind: "code" },
      { key: "normalizedStatus", label: "Provider status", kind: "status" },
      { key: "processingStatus", label: "Processing", kind: "status" },
      { key: "failureReason", label: "Failure reason" },
      { key: "occurredAt", label: "Occurred", kind: "date" },
      { key: "receivedAt", label: "Received", kind: "date" },
      { key: "processedAt", label: "Processed", kind: "date" },
      { key: "rawPayload", label: "Raw payload", kind: "multiline" }
    ],
    relatedLinks: [
      { field: "providerTransactionId", label: "Provider transaction for this ref", resourceKey: "provider-transactions", filter: "providerTransactionId" },
      { field: "providerTransactionId", label: "Settlement records for this provider ref", resourceKey: "settlement-records", filter: "providerTransactionReference" }
    ]
  },
  {
    key: "outbox-events",
    label: "Outbox events",
    navLabel: "Outbox",
    detailLabel: "Outbox event",
    statusField: "status",
    columns: [
      { key: "eventType", label: "Event type" },
      { key: "aggregateId", label: "Aggregate", kind: "code" },
      { key: "status", label: "Status", kind: "status" },
      { key: "attempts", label: "Attempts" },
      { key: "createdAt", label: "Created", kind: "date" }
    ],
    filters: [
      { name: "id", label: "Outbox ID", exact: true },
      { name: "aggregateId", label: "Aggregate ID", exact: true },
      { name: "aggregateType", label: "Aggregate type" },
      { name: "eventType", label: "Event type", exact: true },
      { name: "status", label: "Status", type: "select", options: ["PENDING", "PUBLISHED", "FAILED"] },
      ...lifecycleFilters
    ],
    detailFields: [
      { key: "id", label: "ID", kind: "code" },
      { key: "aggregateType", label: "Aggregate type" },
      { key: "aggregateId", label: "Aggregate ID", kind: "code" },
      { key: "eventType", label: "Event type" },
      { key: "status", label: "Status", kind: "status" },
      { key: "attempts", label: "Attempts" },
      { key: "lastError", label: "Last error" },
      { key: "publishedAt", label: "Published", kind: "date" },
      { key: "nextAttemptAt", label: "Next attempt", kind: "date" },
      { key: "payload", label: "Payload", kind: "multiline" }
    ],
    relatedLinks: [
      {
        field: "aggregateId",
        label: "Open money movement aggregate",
        resourceKey: "money-movements",
        detail: true,
        when: (record) => record.aggregateType === "MoneyMovement"
      }
    ]
  },
  {
    key: "settlement-records",
    label: "Settlement records",
    navLabel: "Settlements",
    detailLabel: "Settlement record",
    statusField: "status",
    columns: [
      { key: "providerTransactionReference", label: "Provider ref", kind: "code" },
      { key: "status", label: "Status", kind: "status" },
      { key: "expectedNetAmount", label: "Expected net", kind: "money" },
      { key: "expectedSettlementDate", label: "Expected date" },
      { key: "createdAt", label: "Created", kind: "date" }
    ],
    filters: [
      { name: "id", label: "Settlement ID", exact: true },
      { name: "moneyMovementId", label: "Movement ID", exact: true },
      { name: "providerTransactionId", label: "Provider txn ID", exact: true },
      { name: "provider", label: "Provider" },
      { name: "status", label: "Status", type: "select", options: ["EXPECTED", "SETTLED", "MISMATCHED"] },
      { name: "providerTransactionReference", label: "Provider ref", exact: true },
      { name: "providerBatchReference", label: "Batch ref", exact: true },
      { name: "expectedSettlementDateFrom", label: "Expected from", type: "date" },
      { name: "expectedSettlementDateTo", label: "Expected to", type: "date" }
    ],
    detailFields: [
      { key: "id", label: "ID", kind: "code" },
      { key: "moneyMovementId", label: "Movement", kind: "code" },
      { key: "providerTransactionId", label: "Provider txn", kind: "code" },
      { key: "status", label: "Status", kind: "status" },
      { key: "expectedGrossAmount", label: "Expected gross", kind: "money" },
      { key: "expectedFeeAmount", label: "Expected fee", kind: "money" },
      { key: "expectedNetAmount", label: "Expected net", kind: "money" },
      { key: "actualNetAmount", label: "Actual net", kind: "money" },
      { key: "providerTransactionReference", label: "Provider ref", kind: "code" },
      { key: "providerBatchReference", label: "Batch ref", kind: "code" }
    ],
    relatedLinks: [
      { field: "moneyMovementId", label: "Open money movement", resourceKey: "money-movements", detail: true },
      { field: "providerTransactionId", label: "Open provider transaction", resourceKey: "provider-transactions", detail: true },
      { field: "providerTransactionReference", label: "Webhook events for this provider ref", resourceKey: "provider-webhook-events", filter: "providerTransactionId" },
      { field: "providerTransactionReference", label: "Reconciliation exceptions for this provider ref", resourceKey: "reconciliation-exceptions", filter: "providerTransactionReference" }
    ]
  },
  {
    key: "reconciliation-runs",
    label: "Reconciliation runs",
    navLabel: "Runs",
    detailLabel: "Reconciliation run",
    statusField: "status",
    columns: [
      { key: "sourceFile", label: "Source file" },
      { key: "status", label: "Status", kind: "status" },
      { key: "totalRows", label: "Total" },
      { key: "exceptionRows", label: "Exceptions" },
      { key: "startedAt", label: "Started", kind: "date" }
    ],
    filters: [
      { name: "id", label: "Run ID", exact: true },
      { name: "status", label: "Status", type: "select", options: ["STARTED", "COMPLETED", "FAILED"] },
      { name: "sourceFile", label: "Source file", exact: true },
      { name: "startedFrom", label: "Started from", type: "datetime-local" },
      { name: "startedTo", label: "Started to", type: "datetime-local" }
    ],
    detailFields: [
      { key: "id", label: "ID", kind: "code" },
      { key: "sourceFile", label: "Source file" },
      { key: "status", label: "Status", kind: "status" },
      { key: "totalRows", label: "Total rows" },
      { key: "matchedRows", label: "Matched rows" },
      { key: "exceptionRows", label: "Exception rows" },
      { key: "startedAt", label: "Started", kind: "date" },
      { key: "completedAt", label: "Completed", kind: "date" },
      { key: "failureReason", label: "Failure reason" }
    ],
    relatedLinks: [
      { field: "id", label: "Exceptions for this run", resourceKey: "reconciliation-exceptions", filter: "reconciliationRunId" }
    ]
  },
  {
    key: "reconciliation-exceptions",
    label: "Reconciliation exceptions",
    navLabel: "Exceptions",
    detailLabel: "Reconciliation exception",
    statusField: "exceptionType",
    columns: [
      { key: "exceptionType", label: "Type", kind: "status" },
      { key: "providerTransactionReference", label: "Provider ref", kind: "code" },
      { key: "provider", label: "Provider" },
      { key: "message", label: "Message" },
      { key: "createdAt", label: "Created", kind: "date" }
    ],
    filters: [
      { name: "id", label: "Exception ID", exact: true },
      { name: "reconciliationRunId", label: "Run ID", exact: true },
      { name: "exceptionType", label: "Type", type: "select", options: ["MISSING_SETTLEMENT", "AMOUNT_MISMATCH", "DUPLICATE_PROVIDER_RECORD"] },
      { name: "provider", label: "Provider" },
      { name: "providerTransactionReference", label: "Provider ref", exact: true },
      ...lifecycleFilters
    ],
    detailFields: [
      { key: "id", label: "ID", kind: "code" },
      { key: "reconciliationRunId", label: "Run", kind: "code" },
      { key: "exceptionType", label: "Type", kind: "status" },
      { key: "provider", label: "Provider" },
      { key: "providerTransactionReference", label: "Provider ref", kind: "code" },
      { key: "message", label: "Message" },
      { key: "rawRecord", label: "Raw record", kind: "multiline" },
      { key: "createdAt", label: "Created", kind: "date" }
    ],
    relatedLinks: [
      { field: "reconciliationRunId", label: "Open reconciliation run", resourceKey: "reconciliation-runs", detail: true },
      { field: "providerTransactionReference", label: "Provider transaction for this ref", resourceKey: "provider-transactions", filter: "providerTransactionId" },
      { field: "providerTransactionReference", label: "Settlement records for this provider ref", resourceKey: "settlement-records", filter: "providerTransactionReference" },
      { field: "providerTransactionReference", label: "Webhook events for this provider ref", resourceKey: "provider-webhook-events", filter: "providerTransactionId" }
    ]
  }
];

export function getOperationsResourceConfig(key?: string) {
  return operationsResources.find((resource) => resource.key === key) ?? operationsResources[0];
}
