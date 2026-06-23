# Week17 Layer-Mix Input Readiness API

## Endpoint

`GET /api/week16/temporal-alignment/layer-mix-input-readiness`

## Purpose

Expose a read-only Java contract that consumes the Mainbase S3-to-W17 layer-mix input readiness artifact.

This endpoint is a readiness gate for the next layer-mix stage. It does not execute a mixer, does not trigger a worker, and does not claim final mix readiness.

## Source artifact

`artifacts/manifests/week17_layer_mix_input_readiness_mainbase_payload.json`

Copied from Mainbase:

`artifacts/evals/week16_s3_to_w17_layer_mix_input.json`

## Response boundary

The response includes:

- `candidateTotal`
- `mixEligibilityCounts`
- `fixtureRoleCounts`
- `blockedClaims`
- `sourceMainbaseDecision`
- copied Mainbase payload
- Java consumer report

## Explicit non-claims

- No real layer mixer was executed.
- No final mix readiness is claimed.
- No semantic audio quality pass is claimed.
- No real worker was triggered.
- No production auth or live service availability is claimed.
