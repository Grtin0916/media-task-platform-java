# Week13 Audio Artifact Registry Contract v0

## Scope

This contract exposes Mainbase Week13 mix placement and dry-run evidence as Java API data.

Endpoint:

- `GET /api/week13/audio-artifacts`
- `GET /api/week13/audio-artifacts/{candidateId}`

## Required fields

- `candidateId`
- `audioUri`
- `sourceType`
- `assetTimeMode`
- `expectedStartSec`
- `expectedEndSec`
- `globalStartSec`
- `globalEndSec`
- `placementOffsetSec`
- `placementRequired`
- `status`

## Runtime rule

- `full_clip`: `globalStartSec = 0`
- `event_local`: `globalStartSec = expectedStartSec`

## Boundary

This is an in-memory API contract over committed Mainbase Week13 evidence.

It does not implement durable artifact storage, database-backed registry, final mixer behavior, semantic quality validation, human audition, or production readiness.
