drop table if exists media_task;

create table media_task (
    id varchar(100) primary key,
    title varchar(255) not null,
    media_type varchar(50) not null,
    status varchar(50) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_media_task_created_at_desc
on media_task (created_at desc);

create index idx_media_task_status_created_at_desc
on media_task (status, created_at desc);

insert into media_task (
    id,
    title,
    media_type,
    status,
    created_at,
    updated_at
) values (
    'week11-k6-seed-created-001',
    'Week11 K6 Seed Created Task',
    'video',
    'CREATED',
    timestamp '2099-05-18 00:00:01',
    timestamp '2099-05-18 00:00:01'
);
