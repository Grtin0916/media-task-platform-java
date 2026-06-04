
# Week12 Audio Candidate API Contract

Generated on: 2026-06-04T13:16:35+00:00

## Endpoint

`GET /api/media-tasks/{taskId}/audio-candidates`

## Purpose

Expose the Mainbase Week12 enriched audio audition review queue through the Java media-task platform.

This is a task-facing API view over candidate metadata. It does not generate audio and does not evaluate semantic audio quality.

## Source artifact

`mainbase:artifacts/evals/week12_audio_audition_review_queue_v0.json`

Imported Java classpath resource:

`src/main/resources/week12-audio-audition-review-queue-v0.json`

## Verified fields

- `taskId`
- `source`
- `schemaVersion`
- `status`
- `qualityGateStatus`
- `reviewQueueArtifactUri`
- `candidateCount`
- `audioProbeOkCount`
- `durationMissingCount`
- `sampleRateMissingCount`
- `eventIdMissingCount`
- `semanticFidelityClaimedAny`
- `mixReadyClaimedAny`
- `doesNotClaim`
- `candidates`

## Boundary

- This is not a complete artifact registry.
- This is not production object storage.
- This is not durable worker orchestration.
- This does not claim semantic audio quality passed.
- This does not claim human audition passed.
- This does not claim final mix readiness.
