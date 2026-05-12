# ADR 0004: API Error Contract and OpenAPI Governance

Date: 2026-05-11

## Status

Accepted for Week10 local contract draft.

## Context

The Java repository already has a minimal Spring Boot service with health, auth, protected media task APIs, database persistence, security baseline, Redis Streams skeleton, Week08 SQL evidence, and Week09 observability / virtual-thread evidence.

Week10 moves from runtime evidence to API contract governance. The repository needs a stable contract for:

- request / response shape
- authentication boundary
- validation error shape
- pagination / sorting / filtering placeholders
- idempotency-key placeholder
- future ContractIT coverage

This ADR does not claim the full implementation is complete. It defines the target contract shape and the boundary for the next implementation step.

## Decision

Use `docs/api/openapi.yaml` as the human-readable and tool-readable API contract source for Week10.

Use a ProblemDetail-style error response for API errors.

Target error fields:

| Field | Meaning | Required |
|---|---|---|
| type | URI or stable problem type | yes |
| title | short human-readable summary | yes |
| status | HTTP status code | yes |
| detail | specific error detail | yes |
| instance | request path or problem instance | yes |
| code | project-specific stable error code | planned |
| traceId | request trace / correlation id | planned |

Initial stable error code families:

| Code | HTTP status | Meaning |
|---|---:|---|
| AUTH_UNAUTHORIZED | 401 | Missing or invalid authentication |
| MEDIA_TASK_INVALID_REQUEST | 400 | Invalid media task request |
| MEDIA_TASK_NOT_FOUND | 404 | Media task does not exist |
| MEDIA_TASK_CONFLICT | 409 | Conflict or future idempotency conflict |
| INTERNAL_ERROR | 500 | Unexpected server-side error |

## Consequences

Positive:

- API behavior can be reviewed without reading controller code.
- ContractIT can later assert current behavior against `docs/api/openapi.yaml`.
- Error response fields stop drifting across endpoints.
- Pagination, sorting, filtering, and idempotency can be introduced incrementally.

Tradeoffs:

- The first OpenAPI file is partly contract-first and partly current-state documentation.
- Some fields are explicitly marked as Week10 placeholders.
- Implementation must not claim support for features that are only documented as future placeholders.

## Current Week10 Boundary

Verified / documented now:

- `/health`
- `/actuator/health`
- `/auth/login`
- `/auth/me`
- `GET /api/media-tasks`
- `POST /api/media-tasks`
- common error response target shape
- pagination / sorting / filtering placeholders
- optional `Idempotency-Key` header placeholder

Not yet verified:

- full JWT issuance / parsing / validation
- production auth policy
- persistent idempotency key enforcement
- formal paged response implementation
- complete error-handler implementation
- ContractIT coverage
- OpenAPI CI validation

## Next Steps

1. Inspect actual controller and model fields.
2. Tighten `docs/api/openapi.yaml` to match implementation exactly.
3. Add a minimal `ContractIT`.
4. Introduce a centralized exception handler only after the documented error shape is agreed.
5. Add a local OpenAPI validation command or Maven plugin later, not in this first Week10 entry.

## Week10 Follow-up: Minimal ProblemDetail Implementation

Implemented and verified on 2026-05-13:

- `IllegalArgumentException` raised by the missing media-task path is handled by `ApiExceptionHandler`.
- The local response shape is ProblemDetail-style and currently includes `type`, `title`, `status`, `detail`, `instance`, and extension field `code`.
- `ContractIT#getMediaTaskByIdShouldReturnProblemDetailWhenMissing` fixes this behavior as the first implementation-backed error contract.

Current boundary:

- This is not a complete production error taxonomy.
- Security-layer `401/403` responses remain handled by Spring Security.
- Validation errors, malformed JSON, traceId propagation, pagination, sorting, filtering, and idempotency remain future work.

