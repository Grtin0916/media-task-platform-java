# Week11 Evidence-Link API Contract

Date: 2026-05-21

## Scope

The Week11 media-task API exposes three V0 evidence-link fields:

- `artifactUri`
- `evalSummaryUri`
- `qualityGateStatus`

These fields are used by the Week11 cross-repo demo to connect:

    Mainbase SoundLayer eval evidence
    -> Java media task API response
    -> Cloud k6 consumer gate

## Field semantics

| Field | Meaning | V0 boundary |
|---|---|---|
| `artifactUri` | Points to the Mainbase eval artifact or manifest associated with a media task. | This is an evidence link, not durable object storage, signed URL management, or a full artifact registry. |
| `evalSummaryUri` | Points to the summary-level eval evidence for downstream consumers. | This is a demo evidence pointer, not a full report service. |
| `qualityGateStatus` | Carries the current V0 quality gate state consumed by Cloud k6. | This is a Week11 gate signal, not a production quality SLA or human perceptual audio score. |

## Contract boundary

This contract is intentionally narrow. It proves that the task platform can expose Mainbase eval evidence to Cloud-side consumers through a stable API shape.

It does not claim:

- complete artifact lifecycle governance;
- persistent artifact registry;
- signed URL or object storage integration;
- production SLO;
- generated-audio perceptual quality benchmark;
- durable async job orchestration.

## Verification

Expected verification command:

    ./mvnw -q -Dtest=ContractIT,MediaTaskQueryHttpIT test

Expected consumer fields:

    artifactUri
    evalSummaryUri
    qualityGateStatus
