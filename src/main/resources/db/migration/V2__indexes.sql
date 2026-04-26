-- Week08 SQL tuning baseline indexes.
-- Each index is tied to one explicit query shape in docs/benchmarks/db_explain_week08.md.
-- Keep this migration narrow: do not add broad indexes without EXPLAIN-backed queries.

create index if not exists idx_media_task_created_at_desc
    on media_task (created_at desc);

create index if not exists idx_media_task_status_created_at_desc
    on media_task (status, created_at desc);

create index if not exists idx_media_asset_task_id_created_at_desc
    on media_asset (task_id, created_at desc);
