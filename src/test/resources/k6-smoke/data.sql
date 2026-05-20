delete from media_task where id = 'week11-k6-seed-created-001';

insert into media_task (id, title, media_type, status, created_at)
values ('week11-k6-seed-created-001', 'Week11 seeded media task', 'video', 'CREATED', CURRENT_TIMESTAMP);
