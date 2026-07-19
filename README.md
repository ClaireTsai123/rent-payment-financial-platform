# Rent Payment Financial Platform

This repository implements the modular Spring Boot Rent Payment Application described in
[`docs/Flex_Rent_Payment_Project_Blueprint.md`](docs/Flex_Rent_Payment_Project_Blueprint.md).
The blueprint is the canonical source of truth for scope, architecture, technology choices, and interview narrative.

## Current Scope

Implemented scope: Phase 1 Tasks 1-5 only.

- Core payment-plan and money-movement persistence model
- Renter collection API that creates and immediately submits a collection money movement for an existing payment plan
- Property disbursement API that creates and immediately submits a separate disbursement money movement for an existing payment plan
- API-level idempotency using `Idempotency-Key`, stable request fingerprints, stored response replay, and conflicting-key rejection
- Provider adapter contract with a deterministic mock provider implementation
- Payment-attempt creation, provider-transaction persistence, provider-result status mapping, money-movement state updates, and state-history append during provider submission
- Payment attempts, provider transaction references, and money-movement state history
- Idempotency and outbox persistence records
- Flyway-managed database schema
- PostgreSQL Testcontainers tests for persistence wiring, key uniqueness constraints, renter collection creation, property disbursement creation, provider submission side effects, and idempotency behavior

Not implemented yet:

- Provider adapter implementation
- Real provider integration
- Webhook ingestion
- Outbox publishing
- SNS/SQS consumers
- Settlement and reconciliation workflows

## Technology

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- Gradle
- JUnit 5
- Testcontainers

The project intentionally does not introduce Kafka, Eureka, OpenFeign, ShardingSphere, Saga, Redis, DynamoDB, Snowflake, Terraform, or AWS CDK for the Phase 1 foundation.

## Local Database

The application defaults to a local PostgreSQL database:

```text
url: jdbc:postgresql://localhost:5432/rent_payment
username: rent_payment
password: rent_payment
```

These can be overridden with:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

Flyway owns schema creation. Hibernate is configured with `ddl-auto=validate`.

## API Idempotency

Collection and disbursement creation endpoints require an `Idempotency-Key` header.
The request body is normalized and fingerprinted for the specific operation. Reusing
the same key with the same completed request returns the original stored response
without creating another money movement. Reusing the same key with a different request
returns `409`.

Idempotency records are uniquely constrained by `(idempotency_key, operation)` in
PostgreSQL. Concurrent duplicate requests are protected by that constraint: one request
creates the in-progress record, while a duplicate in-flight request receives `409`.
Records expire after 24 hours; expired key reuse is rejected with `409`, so callers
must send a fresh `Idempotency-Key`.

## Provider Submission

The Phase 1 vertical slice submits renter collections and property disbursements
immediately through an internal `PaymentProviderAdapter`. The current adapter is a
deterministic mock provider that returns stable mock provider references and normalized
provider statuses.

Mock provider scenarios are selected by `operationKey` so tests and local demos are
repeatable:

- Default operation keys produce an accepted `PROCESSING` provider result.
- Operation keys containing `mock-fail` produce a definitive failure.
- Operation keys containing `mock-timeout` produce an ambiguous timeout.

Provider submission records the first payment attempt, persists the provider transaction,
updates the money-movement state, and appends state history. Ambiguous timeout results
are stored as provider status `UNKNOWN` with the payment attempt marked `AMBIGUOUS`; the
money movement remains `PROCESSING` so a later provider-status verification workflow can
resolve it. The service does not retry ambiguous submissions in this phase.

The public API keeps `operationKey` in the request body. Internally, the provider
idempotency key is currently derived from the same operation key to preserve backward
compatibility and fit the existing provider idempotency column; future provider-specific
adapters can change that derivation behind the adapter boundary.

Webhook processing, retry policy, polling, settlement, and reconciliation are intentionally
left for later Phase 1 tasks.

## Tests

Run all tests:

```bash
./gradlew test
```

Tests use the `test` Spring profile and PostgreSQL Testcontainers so Flyway, Hibernate validation, and database constraints are exercised against PostgreSQL.
