# ADR 0002: Week06 minimal auth strategy

## Status
Accepted (draft)

## Context
Week05 has already landed the PostgreSQL + Flyway + RepositoryIT + `/actuator/prometheus` baseline.
Week06 should not restart database work. The new goal is to add a minimal authentication boundary for login and a protected API surface.

## Decision
This week we choose **minimal JWT** as the primary path.

Fallback:
- if token issuing / parsing / validation blocks progress for too long, temporarily downgrade to **session-based auth**
- the fallback is only for preserving Week06 DoD, not the preferred long-term direction

## Why JWT first
- better bridge to later protected API design
- better bridge to async task APIs and service boundaries
- better bridge to Cloud trace / platform story
- keeps auth semantics explicit instead of being hidden in server session state

## Minimal scope for Week06
- one login entrypoint
- one protected media-task-related endpoint
- one minimal role model:
  - USER
  - ADMIN (can stay reserved in Week06, but role naming should be frozen now)
- one integration test file:
  - happy path
  - unauthorized path (401)

## Proposed endpoint boundary
- login endpoint: `/auth/login`
- protected example endpoint: one existing media-task API path guarded by authentication
- unauthenticated access to protected API should return 401
- README and tests must reflect the same boundary

## Out of scope this week
- refresh token
- external IdP
- OAuth Authorization Server
- Redis-backed session/token store
- complex RBAC / ABAC
- fine-grained permission tree
- token revocation
- multi-tenant auth
- frontend login flow

## Consequences
Positive:
- Week06 gets a stable auth shell without exploding scope
- later async APIs, audit logs, and Cloud tracing can attach to a clear auth boundary

Negative:
- this is intentionally incomplete security
- JWT issuing details may still require follow-up refactor later

## Implementation note for this week
Recommended next files:
- `src/main/java/com/ryan/media/security/SecurityConfig.java`
- `src/main/java/com/ryan/media/auth/AuthController.java`
- `src/test/java/com/ryan/media/AuthIT.java`

Suggested behavior:
- permit `/auth/login`
- require authentication for at least one media-task API
- verify unauthorized requests return 401

## Week06 DoD mapping
This ADR is considered useful only if it directly leads to:
- a login entrypoint
- at least one protected endpoint
- one AuthIT covering success + 401
