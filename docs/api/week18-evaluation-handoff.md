# Week18 evaluation handoff API

Endpoint: `GET /api/week18/evaluation/summary`

Purpose: expose Mainbase W18 DSS-vs-naive evaluation artifacts to the Java task platform as an artifact-backed seed.

Included artifacts:

- W18 evaluation closure summary
- Audio metrics report
- DSS-vs-naive pairwise report
- DSS-aware selector report
- Repair-aware selector seed

Boundary: this endpoint is a local artifact handoff. It does not claim production availability, k6 threshold pass, or live Grafana import.
