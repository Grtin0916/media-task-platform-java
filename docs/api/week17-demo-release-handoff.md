# Week17 Demo Release Handoff API

## Endpoint

`GET /api/week17/demo-release-handoff`

## Purpose

Expose the Mainbase Week17 true-aware demo release candidate as a Java platform handoff artifact.

The endpoint reads:

`artifacts/manifests/week17_demo_release_handoff/week17_demo_release_handoff_report.json`

It does not hard-code a static demo response.

## Claim boundary

This API may claim:

- Mainbase produced a valid release candidate ZIP.
- The ZIP contains `index.html`.
- The ZIP contains at least one WAV fallback.
- The release preserves a single true MMAudio record.

This API must not claim:

- true MMAudio batch success
- full candidate ranking availability
- production SLO verification
- k6 threshold pass
- live Grafana import
- live service availability

`RANDOM_PORT` integration test only validates local HTTP behavior.