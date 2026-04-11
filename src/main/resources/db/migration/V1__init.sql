create table if not exists media_task (
    id varchar(36) primary key,
    title varchar(100) not null,
    media_type varchar(30) not null,
    status varchar(20) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists media_asset (
    id varchar(36) primary key,
    task_id varchar(36) not null references media_task(id) on delete cascade,
    asset_path varchar(512) not null,
    asset_kind varchar(30) not null,
    created_at timestamptz not null default now()
);

create table if not exists media_tag (
    id varchar(36) primary key,
    task_id varchar(36) not null references media_task(id) on delete cascade,
    tag_name varchar(50) not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_media_asset_task_id on media_asset(task_id);
create index if not exists idx_media_tag_task_id on media_tag(task_id);
