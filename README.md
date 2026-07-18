# Rent Payment Financial Platform

This repository implements the modular Spring Boot Rent Payment Application described in
[`docs/Flex_Rent_Payment_Project_Blueprint.md`](docs/Flex_Rent_Payment_Project_Blueprint.md).
The blueprint is the canonical source of truth for scope, architecture, technology choices, and interview narrative.

## Current Scope

Implemented scope: Phase 1 Task 1 only.

- Core payment-plan and money-movement persistence model
- Payment attempts, provider transaction references, and money-movement state history
- Idempotency and outbox persistence records
- Flyway-managed database schema
- PostgreSQL Testcontainers repository tests for persistence wiring and key uniqueness constraints

Not implemented yet:

- Renter collection API
- Property disbursement API
- Provider adapter implementation
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

## Tests

Run all tests:

```bash
./gradlew test
```

Tests use the `test` Spring profile and PostgreSQL Testcontainers so Flyway, Hibernate validation, and database constraints are exercised against PostgreSQL.
