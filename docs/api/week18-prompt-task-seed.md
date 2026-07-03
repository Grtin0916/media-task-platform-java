# Week18 Prompt Task Seed API

## Endpoint

`GET /api/week18/prompt-task-seed`

## Purpose

Expose the Mainbase W18 DSS-vs-naive prompt task queue as a Java platform handoff artifact.

The endpoint reads:

`artifacts/manifests/week18_prompt_task_seed/week18_prompt_task_seed_api_report.json`

It does not hard-code prompt tasks inside the controller.

## What this API proves

- Mainbase produced a 12-task prompt queue.
- Six cases each have `naive` and `dss` prompt variants.
- `glass_drop_room_001` is preserved as a true anchor.
- Repair targets are carried forward into W18.

## What this API must not claim

- true MMAudio batch success
- full candidate ranking availability
- production SLO verification
- k6 threshold pass
- live Grafana import
- live service availability

`RANDOM_PORT` integration test only validates local HTTP behavior.