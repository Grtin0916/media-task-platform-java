# ADR 0001: Database and migration choice for week05 groundwork

## Status
Accepted

## Context
The project has moved beyond a JDBC smoke-only stage.

At the current Week05 stage, the repository already contains:
- PostgreSQL runtime dependency
- Flyway-based schema migration
- JdbcClient-based repository persistence
- RepositoryIT using Testcontainers PostgreSQL
- `/actuator/prometheus` exposure for Cloud-side scraping

The decision we need to freeze is no longer "whether to try a database smoke path",
but "which database + migration + persistence baseline should become the current engineering default".

## Decision
We choose the following Week05 baseline:

1. PostgreSQL as the first relational database
2. Flyway as the schema migration mechanism
3. JdbcClient + explicit repository layer as the current persistence baseline
4. Testcontainers PostgreSQL as the integration-test verification path
5. Spring Boot Actuator + Prometheus registry as the metrics exposure baseline

## Rationale

### Why PostgreSQL
- It is a mainstream production-grade relational database
- It has strong ecosystem support in Spring Boot and Testcontainers
- It is a better long-term baseline than an embedded database for later service evolution

### Why Flyway
- We need versioned, replayable schema changes
- Flyway gives a small and explicit migration surface for the current stage
- The project needs migration history that can be verified in integration tests

### Why JdbcClient first, not full JPA now
- Current schema and domain model are still evolving
- JdbcClient keeps SQL behavior explicit and easy to debug
- It is the shortest path to verify real persistence before introducing a heavier ORM model

### Why RepositoryIT
- We need proof of real persistence against a real PostgreSQL container
- RepositoryIT verifies migration + insert/query/delete behavior together
- This gives Cloud-side observability work a stable backend target

### Why expose `/actuator/prometheus`
- Cloud observability this week depends on a scrapeable application target
- Metrics exposure converts the Java line from "DB-connected" to "Cloud-observable"

## Consequences

### Verified in Week05
- `V1__init.sql` defines the current minimal schema baseline
- `RepositoryIT` verifies real PostgreSQL-backed persistence
- `/actuator/prometheus` returns scrapeable metrics text
- Cloud-side Prometheus can scrape the Java target

### Deferred to later
- richer domain modeling
- security and auth
- Redis / Kafka integration
- production deployment topology
- whether to adopt JPA for more complex domain access later
