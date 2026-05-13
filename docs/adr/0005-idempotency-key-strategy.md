# ADR-0005: Idempotency-Key Strategy for Media Task Creation

Date: 2026-05-14

## Status

Accepted as Week10 contract boundary.

## Context

The Java service already has a Week10 API contract baseline, OpenAPI documentation, ContractIT coverage, and ProblemDetail-style error responses.

The next API governance boundary is idempotency for non-idempotent write operations, especially media task creation. A client may retry a POST request after timeout or network failure. Without an idempotency contract, the same logical request could create duplicate tasks.

## Decision

Use the request header `Idempotency-Key` as the public API contract for future idempotent media task creation.

For Week10, this repository only verifies the contract boundary:

- `Idempotency-Key` is documented as an optional request header for task creation.
- Contract tests may send this header to the create endpoint.
- The service must continue to accept the request when the header is present.
- The current implementation does not claim persistent deduplication.
- The current implementation does not claim replay of the original response.
- The current implementation does not claim exactly-once semantics.

## Intended Future Semantics

A later implementation may persist an idempotency record with:

- idempotency key
- request fingerprint
- operation name
- created resource id
- response status
- response body snapshot or deterministic response reference
- expiration time
- creation/update timestamps

The server should reject reuse of the same key with a different request fingerprint after persistent idempotency is implemented.

## Current Non-Goals

The following are explicitly not verified in Week10:

- Redis-backed idempotency store
- database-backed idempotency table
- distributed locking
- exactly-once task creation
- replaying previous responses
- request fingerprint conflict detection
- expiration and cleanup policy
- cross-instance consistency

## Evidence Plan

Week10 evidence is limited to:

- OpenAPI header documentation
- ContractIT request with `Idempotency-Key`
- test log under `artifacts/logs/`
- README boundary statement

This keeps the repository honest: the API contract is prepared, but production-grade idempotency is not yet claimed.
