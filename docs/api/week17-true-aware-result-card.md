# Week17 True-aware Result Card API

This API exposes the Mainbase true-aware platform bridge as an artifact-backed Java result-card contract.

## Endpoints

- `GET /api/week17/true-aware/result-card/health`
- `GET /api/week17/true-aware/result-card/summary`
- `GET /api/week17/true-aware/result-card/bridge`
- `GET /api/week17/true-aware/result-card/claim-guard`
- `GET /api/week17/true-aware/result-card/artifacts`

## Claim boundary

- true MMAudio single success: `True`
- true MMAudio audio artifact count: `1`
- true MMAudio batch success: `False`
- full candidate ranking available: `False`
- production SLO verified: `False`
- k6 threshold pass verified: `False`

## Platform behavior

Java must expose `safe_true_mmaudio_record_count=1` and keep raw candidate counts as context only.
The API must not inflate the single true V2A candidate into batch success.
