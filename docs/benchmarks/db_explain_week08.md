# Week08 DB EXPLAIN Baseline

Date: 2026-04-27  
Repo: media-task-platform-java  
Stage: Week08 / SQL tuning baseline  
Status: Verified with Testcontainers PostgreSQL 16.13 after explicit ANALYZE

## 1. Purpose

This document records the first SQL tuning baseline for the Java repo.

The goal is not to add many indexes. The goal is to bind each index to one concrete query shape and preserve EXPLAIN ANALYZE evidence.

Current repository facts:

- `media_task` is the core task table.
- `MediaTaskRepository.findAll()` currently reads from `media_task` and sorts by `created_at desc`.
- `MediaTaskRepository.findById()` uses `id`, which is already covered by the primary key.
- `media_asset.task_id` and `media_tag.task_id` already have single-column indexes from `V1__init.sql`.
- Week08 adds only targeted indexes that support ordering, filtering, or task-scoped asset lookup.

## 2. Indexes Added in V2

| Index | Table | Columns | Query shape served |
| --- | --- | --- | --- |
| `idx_media_task_created_at_desc` | `media_task` | `created_at desc` | Recent task list ordered by newest first. |
| `idx_media_task_status_created_at_desc` | `media_task` | `status, created_at desc` | Status-filtered task list ordered by newest first. |
| `idx_media_asset_task_id_created_at_desc` | `media_asset` | `task_id, created_at desc` | Asset lookup for one task ordered by newest asset first. |

## 3. Query A: Recent Task Page

Purpose:

- Matches the current `MediaTaskRepository.findAll()` direction.
- Prepares for later pagination.
- Candidate index: `idx_media_task_created_at_desc`.

SQL:

    explain analyze
    select id, title, media_type, status, created_at
    from media_task
    order by created_at desc
    limit 20;

What to inspect:

- Whether PostgreSQL chooses an index scan.
- Whether the plan still performs an explicit sort.
- Actual rows and actual time.

## 4. Query B: Status-filtered Recent Task Page

Purpose:

- Prepares the likely API shape: filter by task status and show newest tasks first.
- Candidate index: `idx_media_task_status_created_at_desc`.

SQL:

    explain analyze
    select id, title, media_type, status, created_at
    from media_task
    where status = 'CREATED'
    order by created_at desc
    limit 20;

What to inspect:

- Whether the composite index is used.
- Whether filtering and ordering are both supported by the same index.
- Actual rows and actual time.

## 5. Query C: Task-scoped Asset Lookup

Purpose:

- Prepares a task detail page or asset list endpoint.
- Candidate index: `idx_media_asset_task_id_created_at_desc`.

SQL:

    explain analyze
    select id, task_id, asset_path, asset_kind, created_at
    from media_asset
    where task_id = 'task-000001'
    order by created_at desc
    limit 20;

What to inspect:

- Whether PostgreSQL uses the task-scoped composite index.
- Whether the query avoids a separate sort.
- Actual rows and actual time.

## 6. Baseline Commands

Run after the local PostgreSQL database is available and Flyway has applied migrations.

Set connection string:

    export DATABASE_URL='postgresql://media:media@127.0.0.1:5432/media_task'

Check PostgreSQL version:

    psql "$DATABASE_URL" -c "select version();"

Run Query A:

    psql "$DATABASE_URL" -c "
    explain analyze
    select id, title, media_type, status, created_at
    from media_task
    order by created_at desc
    limit 20;
    "

Run Query B:

    psql "$DATABASE_URL" -c "
    explain analyze
    select id, title, media_type, status, created_at
    from media_task
    where status = 'CREATED'
    order by created_at desc
    limit 20;
    "

Run Query C:

    psql "$DATABASE_URL" -c "
    explain analyze
    select id, task_id, asset_path, asset_kind, created_at
    from media_asset
    where task_id = 'task-000001'
    order by created_at desc
    limit 20;
    "

## 7. Evidence Table

Environment:

- Test runner: `QueryPlanIT`
- Database: Testcontainers `postgres:16-alpine`
- PostgreSQL version observed in logs: PostgreSQL 16.13
- Seed data: 5000 rows in `media_task`, 300 rows in `media_asset`
- Statistics refresh: `analyze media_task` and `analyze media_asset` executed after seed data insertion
- Migration status: Flyway applied `V1__init.sql` and `V2__indexes.sql`

| Query | Before or after | Plan summary | Planning time | Execution time | Rows | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Query A: recent task page | after V2 + ANALYZE | `Index Scan using idx_media_task_created_at_desc` | 0.469 ms | 0.056 ms | 20 | Expected index was selected. |
| Query B: status-filtered recent task page | after V2 + ANALYZE | `Index Scan using idx_media_task_created_at_desc` with status filter | 0.082 ms | 0.069 ms | 20 | PostgreSQL did not choose `idx_media_task_status_created_at_desc` in this seed distribution; it scanned newest tasks and removed 40 rows by filter. |
| Query C: task-scoped asset lookup | after V2 + ANALYZE | `Index Scan using idx_media_asset_task_id_created_at_desc` | 0.458 ms | 0.061 ms | 20 | Expected task-scoped composite index was selected. |

## 7.1 Query A Observed Plan

Log file:

- `artifacts/logs/week08_db_explain_query_a_recent_tasks.log`

Observed plan summary:

    Limit  (cost=0.28..1.08 rows=20 width=54) (actual time=0.022..0.030 rows=20 loops=1)
      ->  Index Scan using idx_media_task_created_at_desc on media_task  (cost=0.28..199.28 rows=5000 width=54) (actual time=0.020..0.025 rows=20 loops=1)
    Planning Time: 0.469 ms
    Execution Time: 0.056 ms

Interpretation:

- The newest-first task list is supported by `idx_media_task_created_at_desc`.
- This matches the current `MediaTaskRepository.findAll()` query shape.
- This is the cleanest Week08 index win.

## 7.2 Query B Observed Plan

Log file:

- `artifacts/logs/week08_db_explain_query_b_status_recent_tasks.log`

Observed plan summary:

    Limit  (cost=0.28..2.82 rows=20 width=54) (actual time=0.025..0.042 rows=20 loops=1)
      ->  Index Scan using idx_media_task_created_at_desc on media_task  (cost=0.28..211.78 rows=1666 width=54) (actual time=0.024..0.037 rows=20 loops=1)
            Filter: ((status)::text = 'CREATED'::text)
            Rows Removed by Filter: 40
    Planning Time: 0.082 ms
    Execution Time: 0.069 ms

Interpretation:

- The planner selected `idx_media_task_created_at_desc`, not `idx_media_task_status_created_at_desc`.
- This is still an efficient local plan because the query only needs 20 newest `CREATED` rows and removed 40 rows by filter.
- The composite status/time index remains a candidate for future data distributions where status selectivity is higher or pagination depth changes.
- Do not claim that the composite index eliminated sorting for Query B in the current evidence.

## 7.3 Query C Observed Plan

Log file:

- `artifacts/logs/week08_db_explain_query_c_task_assets.log`

Observed plan summary:

    Limit  (cost=0.27..2.42 rows=20 width=65) (actual time=0.030..0.037 rows=20 loops=1)
      ->  Index Scan using idx_media_asset_task_id_created_at_desc on media_asset  (cost=0.27..32.46 rows=300 width=65) (actual time=0.029..0.032 rows=20 loops=1)
            Index Cond: ((task_id)::text = 'task-000001'::text)
    Planning Time: 0.458 ms
    Execution Time: 0.061 ms

Interpretation:

- The task-scoped asset lookup uses the new composite index.
- This confirms that `idx_media_asset_task_id_created_at_desc` supports both task filtering and newest-first asset listing in the seeded local scenario.
- This index is justified as a Week08 baseline.

## 7.4 Evidence Boundary

The observed timings are local Testcontainers evidence, not production latency.

The current result proves:

- Flyway can apply `V2__indexes.sql`.
- QueryPlanIT can seed deterministic data and emit EXPLAIN ANALYZE logs.
- Query A and Query C selected the expected indexes.
- Query B selected a valid but different low-cost plan.

The current result does not prove:

- production performance improvement;
- performance under large real workloads;
- final API pagination design;
- that every new index must be kept forever.

## 8. Current Non-goals

- Do not add indexes for `findById`; `id` is already the primary key.
- Do not add a status-only index before proving a query shape that needs it.
- Do not tune Redis Streams or eventing in this Week08 DB baseline.
- Do not claim production performance improvement from tiny local data.
- Do not treat EXPLAIN cost as user-visible latency without context.
- Do not modify controller or service code in this step.

## 9. Next Steps

1. Apply Flyway migration locally.
2. Seed enough rows to make query plans meaningful.
3. Run the three EXPLAIN ANALYZE queries.
4. Save output under `artifacts/logs/week08_db_explain_*.log`.
5. Update this document with plan summaries and observed timings.
