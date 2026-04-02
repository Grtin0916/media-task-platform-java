# ADR 0001: Database choice for week05 groundwork

## Status
Accepted

## Context
The repository already has a minimal Spring Boot skeleton, actuator health endpoint, and basic media-task CRUD.
For the next step, we need a real database-backed smoke path before entering full schema migration work in Week05.

## Decision
We choose PostgreSQL as the first relational database for this project.
For the current step, we only verify:
1. Spring Boot test context can start successfully
2. A real PostgreSQL container can be started by Testcontainers
3. The application can obtain a real DataSource connection and execute a minimal SQL query

We intentionally do not introduce full JPA modeling, Flyway/Liquibase migrations, or final repository persistence design in this step.

## Rationale
- PostgreSQL is a mainstream production-grade relational database
- Testcontainers provides a dedicated PostgreSQL module
- Spring Boot provides direct SQL support and Testcontainers integration
- A JDBC-level smoke test is the shortest path to verify real DB connectivity before ORM and migration decisions

## Consequences
Short term:
- We add PostgreSQL runtime dependency
- We add Testcontainers-based integration smoke test
- We keep persistence design lightweight for now

Deferred to Week05:
- schema migration via Flyway or Liquibase
- entity/table design
- repository persistence implementation

Deferred to later:
- Security
- Redis/Kafka
- production deployment topology