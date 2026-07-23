# Rent Payment Financial Platform

This repository implements the modular Spring Boot Rent Payment Application described in
[`docs/Flex_Rent_Payment_Project_Blueprint.md`](docs/Flex_Rent_Payment_Project_Blueprint.md).
The blueprint remains the canonical source of truth for scope, architecture decisions,
technology choices, and interview narrative.

Current implementation checkpoint: **Phase 1 Tasks 1-12** plus full-stack enablement
through the renter and operations portal foundations: local/dev authentication,
production-style OAuth2/JWT resource-server support, renter-scoped read APIs, internal
operations read APIs, seeded local demo data, React workflows, pagination, local
terminal-state demo support under `frontend/` and `scripts/demo/`, and Docker Compose
local full-stack orchestration.

For a fuller Phase 1 documentation checkpoint and full-stack roadmap, see
[`docs/Phase_1_Documentation_Checkpoint.md`](docs/Phase_1_Documentation_Checkpoint.md).

## Project Purpose

The project models the payment-side platform behind a rent-payment product. It focuses
on payment orchestration and financial operations, not underwriting, the renter mobile
app, or complete Billing/Risk/Ledger ownership.

Implemented Phase 1 flows:

- Store payment-side snapshots of upstream Billing payment plans.
- Create renter collection money movements against existing payment plans.
- Create property disbursement money movements against existing payment plans.
- Submit those movements to a deterministic mock provider adapter.
- Track attempts, provider references, state history, webhooks, outbox events,
  settlement expectations, and reconciliation outcomes.
- Authenticate renter-facing and internal command APIs through either the replaceable
  local/dev/test bearer-token principal or production-style OAuth2/JWT resource-server
  authentication.
- Expose renter-scoped read APIs for payment plans and money movements.
- Provide a local/dev React renter portal foundation that reads renter-scoped plans and
  money movements and initiates renter collections through the existing command API.
- Seed local/dev-only demo payment plans and money movements for `renter-123` so the
  renter portal can be exercised end to end.

Billing, Risk, and Ledger remain external/shared boundaries. The application does not
create unnecessary microservices and does not introduce Kafka, Eureka, OpenFeign,
ShardingSphere, Saga, Redis, DynamoDB, Snowflake, Terraform, or AWS CDK for Phase 1.

## Current Architecture

```mermaid
flowchart TD
    Client["API client or future portals"] --> API["Rent Payment Application"]
    API --> Collection["Collection module"]
    API --> Disbursement["Disbursement module"]
    Collection --> Provider["PaymentProviderAdapter"]
    Disbursement --> Provider
    Provider --> MockProvider["Deterministic mock provider"]
    API --> DB[("PostgreSQL")]
    API --> Webhook["Mock-provider webhook ingestion"]
    API --> Outbox["Transactional outbox"]
    Outbox --> Publisher["Scheduled local mock publisher"]
    DB --> Batch["Spring Batch reconciliation job"]
    File["Local S3-style settlement CSV"] --> Batch
```

The code is one Spring Boot deployable organized into explicit modules:

- `paymentplan`: payment-side snapshot of upstream Billing obligations
- `collection`: renter collection command API
- `disbursement`: property disbursement command API
- `provider`: provider adapter contract, normalized models, mock adapter
- `webhook`: mock-provider webhook verification, persistence, deduplication
- `idempotency`: stable request fingerprinting and replay handling
- `outbox`: transactional event persistence and scheduled local publishing
- `settlement`: expected settlement record creation
- `reconciliation`: chunk-oriented Spring Batch settlement-file reconciliation
- `security`: stateless Spring Security configuration and replaceable dev principal
- `renter`: renter-scoped portal read/query APIs and DTOs
- `operations`: secured internal read/query APIs and DTOs for support and finance users
- `shared`: domain enums, money movement entities, state-transition service, API errors

The repository also contains one frontend application:

- `frontend`: React, TypeScript, Vite, React Router, TanStack Query renter and internal
  operations portal foundation for local/dev full-stack validation, including
  URL-shareable operations investigation views

Local/dev profiles include demo fixture data:

- `renter-123` has realistic active and completed payment plans.
- Seeded plan and money-movement rows use stable demo keys and are safe to rerun without
  creating duplicates.
- This fixture is for local portal validation only; production plan creation remains a
  Billing-owned boundary outside this service.

## End-to-End Flows

### Renter Collection

```mermaid
sequenceDiagram
    participant Client
    participant API as RenterCollectionController
    participant Idem as IdempotencyService
    participant Provider as Mock Provider Adapter
    participant DB as PostgreSQL

    Client->>API: POST /api/v1/renter-collections
    API->>Idem: startOrReplay(Idempotency-Key, operation)
    Idem->>DB: insert or read idempotency record
    API->>DB: load existing PaymentPlan
    API->>DB: create MoneyMovement and PaymentAttempt
    API->>Provider: submit normalized collection request
    Provider-->>API: PROCESSING, FAILED, or UNKNOWN
    API->>DB: persist ProviderTransaction
    API->>DB: transition MoneyMovement and append history
    API->>DB: persist OutboxEvent for meaningful state change
    API->>Idem: store completed response
    API-->>Client: stored command response
```

The collection API assumes the `PaymentPlan` already exists. PaymentPlan creation belongs
to the upstream Billing domain and is intentionally out of scope for this service.

### Property Disbursement

Property disbursement follows the same command pattern but is now treated as an internal
financial-operations command rather than a renter-facing action. It creates an independent
`PROPERTY_DISBURSEMENT` money movement. The disbursement amount comes from the
payment-side `PaymentPlan` rent amount, while renter collection uses the initial
collection amount.

```mermaid
flowchart LR
    Plan["Existing PaymentPlan"] --> Collection["RENTER_COLLECTION MoneyMovement"]
    Plan --> Disbursement["PROPERTY_DISBURSEMENT MoneyMovement"]
    Collection --> Attempt1["PaymentAttempt"]
    Disbursement --> Attempt2["PaymentAttempt"]
    Attempt1 --> ProviderTxn1["ProviderTransaction"]
    Attempt2 --> ProviderTxn2["ProviderTransaction"]
```

## Provider Adapter And Result Handling

The provider boundary is represented by `PaymentProviderAdapter` plus normalized
request/response records. The current implementation is a deterministic mock provider.

Mock provider scenarios are selected by `operationKey`:

- Default keys return an accepted `PROCESSING` provider result.
- Keys containing `mock-fail` return a definitive failure.
- Keys containing `mock-timeout` return an ambiguous timeout.

Provider submission persists:

- `PaymentAttempt`
- `ProviderTransaction`
- `MoneyMovement` state update
- `MoneyMovementStateHistory`
- `OutboxEvent` for meaningful movement state changes

Ambiguous timeout is not blindly marked failed and is not retried in Phase 1. It is stored
as provider status `UNKNOWN` with an ambiguous payment attempt, leaving final resolution
to a later provider-status verification or webhook workflow.

## Idempotency

Collection and disbursement endpoints require:

```text
Idempotency-Key: <stable client key>
```

The JSON body still carries `operationKey` for backward compatibility and provider
idempotency derivation.

Implemented behavior:

- Database uniqueness on `(idempotency_key, operation)`
- Stable normalized request fingerprinting
- Insert-race handling for concurrent duplicate requests
- Completed duplicate request replay returns the original stored response
- Conflicting key reuse returns `409`
- In-flight duplicate request returns `409`
- Expired key reuse returns `409`
- Expiration timestamp is persisted on `IdempotencyRecord`

## Authentication And Authorization

Spring Security is configured as a stateless API security boundary. It intentionally
does not implement registration, password management, token issuance, or a full identity
service.

Local/dev/test requests use a replaceable bearer-token principal:

```text
Authorization: Bearer dev:<subject>:<renterId>:<comma-separated-roles>
```

Examples:

```text
Authorization: Bearer dev:test-user:renter-123:RENTER
Authorization: Bearer dev:finops-user:-:FINOPS
```

For profiles outside `local`, `dev`, and `test`, the application uses Spring Security
OAuth2 Resource Server JWT authentication. Configure JWT validation with one of Spring
Boot's environment-backed settings:

```text
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://issuer.example.com/
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=https://issuer.example.com/.well-known/jwks.json
```

JWT claim mapping:

- subject: `sub`
- renter scope: `renter_id` or `renterId`
- roles: `roles`, `authorities`, `scope`, or `scp`
- supported roles: `RENTER`, `SUPPORT`, `FINOPS`, `ADMIN`

The JWT converter maps those claims into the same internal `ApplicationUser` model used
by the dev-token flow, so renter ownership enforcement and method-level role rules are
shared.

Implemented authorization behavior:

- `/api/v1/me/**` requires role `RENTER`.
- Renter collection requires role `RENTER`.
- Property disbursement requires role `FINOPS` or `ADMIN`.
- Renter command and read APIs derive renter scope from the authenticated principal.
- A renter cannot read or create a movement against another renter's payment plan.
- Property disbursement does not derive renter scope from the authenticated principal;
  it loads the existing payment plan as an internal financial-operations command.
- `/api/v1/provider-webhooks/mock-provider` remains protected by provider shared-secret
  verification, not renter JWT authentication.
- `/actuator/health` is public for local health checks.
- The dev bearer-token filter is profile-gated to `local`, `dev`, and `test`; it is not
  registered for production profiles.
- Production-style profiles reject `dev:` tokens and expect JWT bearer tokens from the
  configured issuer or JWK set.

## Webhook Ingestion

Mock-provider webhooks are accepted at:

```text
POST /api/v1/provider-webhooks/mock-provider
X-Mock-Provider-Signature: <shared secret>
```

Payload:

```json
{
  "providerEventId": "event-123",
  "providerTransactionId": "mock-txn-123",
  "providerStatus": "SUCCEEDED",
  "occurredAt": "2026-08-01T12:00:00Z"
}
```

Implemented behavior:

- Minimal shared-secret signature verification
- Raw payload persistence in `provider_webhook_events`
- Database uniqueness on `(provider, provider_event_id)`
- Idempotent duplicate delivery handling
- Provider transaction lookup
- Updates to `ProviderTransaction`, `PaymentAttempt`, `MoneyMovement`, and state history
- Terminal-state and invalid-regression protection
- Unknown provider transactions are persisted as `UNMATCHED`
- Ignored invalid-regression events remain available for investigation
- `occurredAt`-based stale-event detection remains future work

## State Transitions

`MoneyMovementStateTransitionService` centralizes:

- Valid transition rules
- MoneyMovement mutation
- State-history creation
- Invalid-regression rejection
- No-op transition handling
- Transactional outbox event creation for meaningful state changes

Same-state transitions such as `PROCESSING -> PROCESSING` do not create duplicate
state-history rows by default. Webhook audit details remain in `ProviderWebhookEvent`.

## Transactional Outbox

Meaningful money-movement state changes create an `OutboxEvent` in the same PostgreSQL
transaction as the movement update and state-history row.

Outbox events use:

- aggregate type: `MoneyMovement`
- aggregate ID: the money movement ID
- event type: `money-movement.state-changed`
- payload: stable JSON stored at transition time
- status: `PENDING`
- attempts: `0`
- `next_attempt_at` for retry eligibility

No event is emitted for no-op transitions, rejected transitions, duplicate webhook replay,
unmatched webhooks, ignored invalid-regression webhooks, or idempotent API replay.

## Scheduled Outbox Publishing

`ScheduledOutboxPublisher` polls eligible pending outbox rows and delegates to
`OutboxPublisher`.

Implemented behavior:

- PostgreSQL `FOR UPDATE SKIP LOCKED` claiming to avoid duplicate publication from
  overlapping scheduler executions
- Local mock `EventPublisher`
- Stored payload is published without rebuilding it
- Successful rows become `PUBLISHED` with `published_at`
- Failed rows increment `attempts`, store `last_error`, and set `next_attempt_at`
- Exhausted rows become `FAILED`

Real SNS/SQS publishing and consumers remain future production-direction work.

## Settlement Expectations

When a provider-backed money movement reaches `SUCCEEDED`, the application creates one
`SettlementRecord` with status `EXPECTED`.

The record captures:

- expected gross amount
- expected fee amount
- expected net amount
- currency
- expected settlement date
- provider
- provider transaction reference

Duplicate webhook replay and already-existing settlement expectations do not create
additional settlement rows.

## Spring Batch Reconciliation

`providerSettlementReconciliationJob` is a chunk-oriented Spring Batch job that reads
provider settlement CSV files through a `SettlementFileSource` abstraction. The current
implementation resolves local files; the interface is ready for a future S3 source.

Current CSV format:

```text
provider,providerTransactionReference,grossAmount,feeAmount,netAmount,currency,settlementDate,providerBatchReference
mock-provider,mock-txn-123,500.00,0.00,500.00,USD,2026-08-02,batch-001
```

The job uses:

- `FlatFileItemReader`
- `ItemProcessor`
- `ItemWriter`
- configurable chunk size
- `ReconciliationRun` source-file identity
- `ReconciliationExceptionRecord` for operations review

Implemented reconciliation outcomes include matched rows, missing internal settlements,
amount mismatches, duplicate provider records, malformed-file failure, completed rerun
deduplication, and failed-run restartability.

## Persistence And Flyway

Flyway owns schema creation. Hibernate runs with `ddl-auto=validate`.

Current migrations:

| Migration                                | Scope                                                                                               |
|------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `V1__create_payment_core_tables.sql`     | payment plans, money movements, attempts, provider transactions, state history, idempotency, outbox |
| `V2__create_provider_webhook_events.sql` | webhook event audit and provider event deduplication                                                |
| `V3__create_settlement_records.sql`      | expected and actual settlement tracking                                                             |
| `V4__create_reconciliation_tables.sql`   | reconciliation runs and exception records                                                           |
| `V5__add_operations_read_indexes.sql`    | read-side indexes for internal operations filtering and newest-first list views                      |

Important constraints include:

- `payment_plans.billing_obligation_id`
- `money_movements.operation_key`
- `payment_attempts(money_movement_id, attempt_number)`
- `provider_transactions(provider, provider_transaction_id)`
- `provider_transactions(provider, provider_idempotency_key)`
- `idempotency_records(idempotency_key, operation)`
- `provider_webhook_events(provider, provider_event_id)`
- `settlement_records.money_movement_id`
- `settlement_records.provider_transaction_id`
- `reconciliation_runs.source_file`
- `reconciliation_exceptions(reconciliation_run_id, provider, provider_transaction_reference, exception_type)`

## Current API Endpoints

Renter-facing collection and `/api/v1/me/**` read endpoints require:

```text
Authorization: Bearer dev:<subject>:<renterId>:RENTER
```

### Renter Collection

```http
POST /api/v1/renter-collections
Authorization: Bearer dev:test-user:renter-123:RENTER
Idempotency-Key: <key>
Content-Type: application/json

{
  "paymentPlanId": "<uuid>",
  "operationKey": "collection-001",
  "currency": "USD"
}
```

Response:

```json
{
  "moneyMovementId": "<uuid>",
  "paymentPlanId": "<uuid>",
  "type": "RENTER_COLLECTION",
  "state": "PROCESSING",
  "amount": 500.00,
  "currency": "USD",
  "operationKey": "collection-001",
  "createdAt": "2026-08-01T12:00:00Z"
}
```

### Property Disbursement

```http
POST /api/v1/property-disbursements
Authorization: Bearer dev:finops-user:-:FINOPS
Idempotency-Key: <key>
Content-Type: application/json

{
  "paymentPlanId": "<uuid>",
  "operationKey": "disbursement-001",
  "currency": "USD"
}
```

### Mock Provider Webhook

```http
POST /api/v1/provider-webhooks/mock-provider
X-Mock-Provider-Signature: local-mock-webhook-secret
Content-Type: application/json

{
  "providerEventId": "event-001",
  "providerTransactionId": "mock-txn-001",
  "providerStatus": "SUCCEEDED",
  "occurredAt": "2026-08-01T12:00:00Z"
}
```

Response:

```json
{
  "result": "APPLIED"
}
```

Webhook response results:

- `APPLIED`: the webhook matched a provider transaction and applied a valid state update.
- `DUPLICATE`: the provider event was already received and no second update was applied.
- `UNMATCHED`: the provider transaction was unknown; the raw event was retained for investigation.
- `IGNORED`: the event matched a provider transaction but was not allowed to regress or mutate a terminal state.

### Renter Portal Read APIs

```http
GET /api/v1/me/payment-plans?page=0&size=20&sort=createdAt,desc
Authorization: Bearer dev:test-user:renter-123:RENTER
```

```http
GET /api/v1/me/payment-plans/{paymentPlanId}
Authorization: Bearer dev:test-user:renter-123:RENTER
```

```http
GET /api/v1/me/money-movements?page=0&size=20
Authorization: Bearer dev:test-user:renter-123:RENTER
```

```http
GET /api/v1/me/money-movements?paymentPlanId={paymentPlanId}&page=0&size=20
Authorization: Bearer dev:test-user:renter-123:RENTER
```

```http
GET /api/v1/me/money-movements/{moneyMovementId}
Authorization: Bearer dev:test-user:renter-123:RENTER
```

These APIs return Spring `Page` responses for list endpoints and DTOs rather than JPA
entities. List endpoints default to `size=20` and newest-first sorting by `createdAt`
descending. Requested page sizes are capped at 100.

### Internal Operations Read APIs

Internal operations APIs are read-only and require `SUPPORT`, `FINOPS`, or `ADMIN`.
They return DTOs rather than JPA entities, default list responses to `size=20`, cap
requested page sizes at 100, and default to newest-first sorting where the underlying
record has a lifecycle timestamp.

```http
GET /api/v1/ops/money-movements
GET /api/v1/ops/money-movements/{id}
GET /api/v1/ops/provider-transactions
GET /api/v1/ops/provider-transactions/{id}
GET /api/v1/ops/provider-webhook-events
GET /api/v1/ops/provider-webhook-events/{id}
GET /api/v1/ops/outbox-events
GET /api/v1/ops/outbox-events/{id}
GET /api/v1/ops/settlement-records
GET /api/v1/ops/settlement-records/{id}
GET /api/v1/ops/reconciliation-runs
GET /api/v1/ops/reconciliation-runs/{id}
GET /api/v1/ops/reconciliation-exceptions
GET /api/v1/ops/reconciliation-exceptions/{id}
Authorization: Bearer dev:support-user:-:SUPPORT
```

Practical filters include:

- money movements: `id`, `paymentPlanId`, `renterId`, `state`, `type`, `operationKey`,
  `createdFrom`, `createdTo`
- provider transactions: `id`, `moneyMovementId`, `paymentAttemptId`, `provider`,
  `status`, `providerTransactionId`, `providerIdempotencyKey`, `createdFrom`,
  `createdTo`
- provider webhook events: `id`, `provider`, `providerEventId`,
  `providerTransactionId`, `normalizedStatus`, `status`, `occurredFrom`, `occurredTo`,
  `receivedFrom`, `receivedTo`
- outbox events: `id`, `aggregateId`, `aggregateType`, `eventType`, `status`,
  `createdFrom`, `createdTo`
- settlement records: `id`, `moneyMovementId`, `providerTransactionId`, `provider`,
  `status`, `providerTransactionReference`, `providerBatchReference`,
  `expectedSettlementDateFrom`, `expectedSettlementDateTo`, `actualSettlementDateFrom`,
  `actualSettlementDateTo`, `createdFrom`, `createdTo`
- reconciliation runs: `id`, `status`, `sourceFile`, `startedFrom`, `startedTo`,
  `completedFrom`, `completedTo`
- reconciliation exceptions: `id`, `reconciliationRunId`, `exceptionType`, `provider`,
  `providerTransactionReference`, `createdFrom`, `createdTo`

Detail endpoints return clear `OPERATIONS_RESOURCE_NOT_FOUND` errors for unknown IDs.
Invalid enum/date parameters return `INVALID_REQUEST_PARAMETER`; invalid date ranges
return `INVALID_FILTER`.

## Local Development

Frontend development is standardized on **Node 22 LTS**. The repository includes a
root `.nvmrc`; do not use Node 23 for frontend install, test, lint, build, or dev-server
work.

```bash
nvm install
nvm use
node --version
```

The frontend `package.json` enforces `node >=22.13.0 <23`.

The application defaults to local PostgreSQL:

```text
url: jdbc:postgresql://localhost:5432/rent_payment
username: rent_payment
password: rent_payment
```

Environment overrides:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
MOCK_PROVIDER_WEBHOOK_SECRET
OUTBOX_PUBLISHER_BATCH_SIZE
OUTBOX_PUBLISHER_MAX_ATTEMPTS
OUTBOX_PUBLISHER_RETRY_DELAY
OUTBOX_PUBLISHER_FIXED_DELAY
OUTBOX_PUBLISHER_SCHEDULER_ENABLED
RECONCILIATION_SETTLEMENT_CHUNK_SIZE
```

Run tests:

```bash
./gradlew clean test --no-daemon
```

Run the application after PostgreSQL is available:

```bash
./gradlew bootRun
```

For local full-stack development, `bootRun` activates the `local` Spring profile by
default so the replaceable dev bearer-token filter is registered. Override it explicitly
when needed:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
./gradlew bootRun -Dspring.profiles.active=prod
```

Run the renter portal after installing frontend dependencies:

```bash
cd frontend
nvm use
npm install
npm run dev
```

The Vite dev server proxies `/api` and `/actuator` to `http://localhost:8080`, so the
current repository does not need backend CORS changes for local full-stack development.
Use a local/dev renter token such as:

```text
dev:test-user:renter-123:RENTER
```

With the `local` or `dev` profile active, startup seeds demo data for `renter-123`.
Use that token to see populated plans, existing money movements, payment-plan detail, and
collection submission feedback in the portal.

### Local Collection Terminal-State Demo

The current repository intentionally keeps provider terminal-state changes behind the
mock-provider webhook contract. The renter portal can initiate a collection and refresh
the read model, but it does not expose provider transaction identifiers or privileged
webhook controls.

For local/dev validation only, use the helper script to move a newly submitted collection
from `PROCESSING` to a terminal provider status through the existing webhook endpoint:

```bash
./gradlew bootRun
```

In another terminal:

```bash
cd frontend
nvm use
npm run dev
```

Sign in to the portal with:

```text
dev:test-user:renter-123:RENTER
```

Submit a collection from an eligible active payment plan, then read the latest local demo
collection operation key:

```bash
OPERATION_KEY="$(
  PGPASSWORD=rent_payment psql -h localhost -U rent_payment -d rent_payment -Atc "
    select mm.operation_key
    from money_movements mm
    join payment_plans pp on pp.id = mm.payment_plan_id
    where pp.renter_id = 'renter-123'
      and mm.type = 'RENTER_COLLECTION'
    order by mm.created_at desc
    limit 1;
  "
)"
```

Apply a terminal mock-provider webhook:

```bash
scripts/demo/mock-provider-webhook.sh --operation-key "$OPERATION_KEY" --status SUCCEEDED
```

Supported demo statuses are `SUCCEEDED`, `FAILED`, `RETURNED`, and `REVERSED`. Refresh the
portal after the webhook to see the updated money-movement status. The script defaults to
`http://localhost:8080` and refuses non-local API URLs unless explicitly overridden for an
intentional sandbox.

Production architecture should resolve provider terminal states through verified provider
webhooks, status polling/reconciliation, operations runbooks, and auditable internal tools,
not through renter-facing controls or local database lookups.

### Docker Compose Local Full Stack

The repository also includes a Docker Compose workflow for local full-stack validation.
It preserves the non-Docker Gradle and Vite workflow above.

Create a local environment file:

```bash
cp .env.example .env
```

The example values are development-only placeholders. Compose starts PostgreSQL, the
Spring Boot backend with the `local` profile, and an Nginx-served frontend built with
Node 22. The frontend proxies `/api` and `/actuator` to the backend service inside the
Compose network.

Build and start all services:

```bash
docker compose build
docker compose up -d
docker compose ps
```

Default local ports:

```text
PostgreSQL: localhost:5432
Backend:    http://localhost:8080
Frontend:   http://localhost:5173
```

Check health and open the portal:

```bash
curl http://localhost:8080/actuator/health
open http://localhost:5173
```

Use local/dev tokens in the Dockerized frontend:

```text
Renter:     dev:test-user:renter-123:RENTER
Operations: dev:support-user:-:SUPPORT
Operations: dev:finops-user:-:FINOPS
Operations: dev:admin-user:-:ADMIN
```

View logs:

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres
```

Rebuild after code changes:

```bash
docker compose up -d --build
```

Stop services without deleting the database volume:

```bash
docker compose down
```

Reset all local Docker data, including the named PostgreSQL volume:

```bash
docker compose down -v
```

## Testing Strategy

Current tests use JUnit 5 and PostgreSQL Testcontainers. They exercise Flyway migrations,
Hibernate validation, repository constraints, command APIs, idempotency behavior, provider
submission side effects, webhook deduplication, state transitions, transactional outbox,
outbox publisher retries/concurrency, settlement expectation creation, and chunk-oriented
reconciliation restart behavior. The full-stack enablement tests also cover stateless
dev-principal authentication, role rejection, renter ownership scoping, and paginated
renter read APIs.

Current test classes:

- `RentPaymentFinancialPlatformApplicationTests`
- `PaymentCorePersistenceTests`
- `RenterCollectionControllerTests`
- `PropertyDisbursementControllerTests`
- `IdempotencyServiceTests`
- `MockProviderWebhookControllerTests`
- `MoneyMovementStateTransitionServiceTests`
- `OutboxPublisherTests`
- `ReconciliationJobTests`
- `RenterPortalControllerTests`
- `OperationsReadControllerTests`
- `ProductionJwtSecurityTests`
- `LocalDevSecurityFlowTests`

## Implemented Versus Not Yet Implemented

Implemented:

- Phase 1 Tasks 1-12
- First full-stack enablement slice: Spring Security dev principal, renter-scoped read
  APIs, DTOs, pagination, and renter ownership checks
- Production-style OAuth2/JWT resource-server authentication path
- Internal operations read APIs for support and finance workflows
- Read-only internal operations portal foundation under `frontend/`
- URL-shareable operations portal investigation views with synchronized filters,
  pagination, exact-search inputs, and related-record navigation
- Docker Compose local full-stack orchestration for PostgreSQL, backend, and frontend
- Local/dev-only demo seed data for `renter-123`
- Modular Spring Boot backend
- PostgreSQL/Flyway persistence
- Command APIs for collection and disbursement
- Deterministic mock provider adapter
- Idempotency, webhook deduplication, state transitions, outbox, local publishing,
  settlement expectation, and chunk-oriented reconciliation
- PostgreSQL-backed integration testing

Not yet implemented:

- Real payment provider integration
- Provider status polling/resynchronization
- Scheduled renter repayment
- ACH return flow
- Refund and reversal command flows
- Real SNS/SQS publisher and consumers
- Processed-event SQS consumer deduplication table
- DLQ operational workflow
- Real S3 file source
- Production-grade renter-facing portal
- Production-grade internal financial-operations portal
- Production deployment artifacts

## Production Architecture Direction

The blueprint direction is service-oriented, not all-microservices:

```mermaid
flowchart TD
    Portal["Renter Portal / Operations Portal"] --> API["Rent Payment Application"]
    API --> Billing["Billing service boundary"]
    API --> Risk["Risk service boundary"]
    API --> Provider["Payment provider or bank"]
    API --> Aurora[("Aurora PostgreSQL")]
    API --> Outbox["Transactional outbox"]
    Outbox --> SNS["Amazon SNS"]
    SNS --> SQS["Amazon SQS queues"]
    SQS --> Worker["Financial Operations Worker"]
    Worker --> Ledger["Ledger platform"]
    Worker --> DLQ["SQS DLQ and alerts"]
    S3["S3 settlement files"] --> Batch["Spring Batch reconciliation"]
    Aurora --> Batch
    Ledger --> Batch
```

Future production concerns:

- S3-backed `SettlementFileSource`
- SNS/SQS publishing and idempotent consumers
- DLQs with operational alerting and replay procedures
- Secure secrets through environment or managed secret stores
- OAuth2/JWT authentication and role-based authorization
- Renter ownership enforcement
- Metrics, tracing, structured logs, and dashboards through Actuator, Micrometer,
  Datadog, and CloudWatch
- Containerized deployment through Docker/ECR/EKS
- Clear runbooks for ambiguous provider outcomes, webhook replay, outbox failures,
  settlement exceptions, and reconciliation failures

## Proposed Full-Stack Roadmap

Recommended frontend stack:

- React
- TypeScript
- Vite
- React Router
- TanStack Query
- A restrained component system suitable for financial operations screens

Proposed frontend structure:

```text
frontend/
  src/
    app/
    auth/
    api/
    pages/
      renter/
      ops/
    components/
      layout/
      tables/
      filters/
      status/
      money/
      forms/
      feedback/
    hooks/
    utils/
```

Current frontend implementation:

- Local/dev token entry and storage for the existing bearer-token filter
- Protected renter portal shell
- Dashboard listing `/api/v1/me/payment-plans` and `/api/v1/me/money-movements`
- Payment-plan detail page with plan-scoped movement history
- Money-movement detail page
- Renter collection initiation using `POST /api/v1/renter-collections` with an
  idempotency key
- Collection success and backend error feedback with refresh after submission
- Pagination controls for payment plans, dashboard money movements, and plan-scoped
  movement history using Spring `Page` metadata
- Focused Vitest/React Testing Library coverage for renter dashboard, payment-plan
  detail, money-movement detail, auth headers, pagination, success, empty, and error
  states
- Operations login support using the same local/dev token entry, including `SUPPORT`,
  `FINOPS`, and `ADMIN` route protection
- Read-only operations navigation, list pages, detail pages, pagination, filters,
  loading/empty/error/background-refresh states, state-history detail, and reusable
  table/filter/detail rendering for `/api/v1/ops/**`
- URL query parameters synchronize operations filters and pagination, so filtered views
  survive refresh, browser navigation, and shared links. This is the current saved-view
  capability; there are no persisted or named saved views.
- Exact-search fields are called out for IDs, operation keys, idempotency keys, provider
  references, settlement references, source files, and reconciliation run references.
- Operations detail pages include related-record links between money movements, provider
  transactions, webhook events, outbox events, settlement records, reconciliation runs,
  and reconciliation exceptions.
- Focused operations portal tests for role protection, list rendering, filtering,
  pagination, URL-filter persistence, shared deep links, detail navigation, and backend
  error handling
- Vite dev proxy for local Spring Boot integration
- Local/dev terminal-state demo script for applying mock-provider webhooks to portal-created
  collections without adding privileged controls to the renter UI

Current internal operations backend implementation:

- Secured read-only `/api/v1/ops/**` endpoints for money movements, provider
  transactions, webhook events, outbox events, settlement records, reconciliation runs,
  and reconciliation exceptions
- DTO-only responses with pagination, exact-match filters, bounded date/range filters,
  detail views, and state-history detail for money movements
- Focused backend integration coverage for operations authorization, filtering,
  pagination, detail lookup, not-found responses, and invalid filters

Recommended full-stack phases:

1. Backend read-model APIs for renter payment plans and money movements.
2. Spring Security with JWT-style local/dev auth and roles: `RENTER`, `SUPPORT`,
   `FINOPS`, `ADMIN`.
3. Renter portal vertical slice: dashboard, payment-plan detail, money-movement history,
   and collection initiation through the existing command API.
4. Operations portal read-only slice: money movements, provider transactions, webhook
   events, outbox events, settlement records, reconciliation runs, and exceptions.
5. Pagination, filtering, constrained search, and supporting database indexes.
6. Provider ambiguity/status-polling runbooks and internal investigation tools.
7. Manual-resolution APIs only after read-only operations workflows remain stable.
8. Production integrations: S3, SNS/SQS, DLQ workflows, secure secrets, observability,
   and deployment artifacts.

Recommended first full-stack vertical slice:

- Add authenticated renter-scoped read APIs.
- Build a renter dashboard that lists the renter's payment plans and money movements.
- Reuse the existing renter collection command endpoint with idempotency.
- Keep payment execution, webhook, outbox, settlement, and reconciliation domain logic
  unchanged.
