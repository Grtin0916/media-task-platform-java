# Artifact-backed repair review workflow

The workflow imports Mainbase's immutable `repair_handoff_20260715.json`. Java verifies
path containment, regular-file status, SHA-256 and size before creating records. It
does not recompute audio metrics or execute DSP.

The model keeps two independent axes:

- `workflowState`: Java import, integrity and review progress.
- `repairDecision`: the upstream repair decision, preserved at import.

`MANUAL_REVIEW` can transition only after a complete human review containing
preference, reason, confidence, reviewer and a non-unknown forbidden-event label.
`REPAIR_REJECTED` and `REPAIR_BLOCKED` cannot be promoted.

Endpoints:

- `POST /api/repair-workflows/import`
- `GET /api/repair-workflows/{batchId}`
- `GET /api/repair-workflows/{batchId}/summary`
- `GET /api/repair-records`
- `GET /api/repair-records/{repairId}`
- `POST /api/repair-records/{repairId}/reviews`
- `GET /api/repair-records/{repairId}/history`

Detail responses include an ETag. Review submission accepts `If-Match` and requires
the current `reviewVersion`. This is an in-process lost-update guard, not a
distributed exactly-once guarantee.
