create table if not exists media_task (
  id varchar(255) primary key,
  title varchar(255) not null,
  media_type varchar(50) not null,
  status varchar(50) not null,
  created_at timestamp not null
);
