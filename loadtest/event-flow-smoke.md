# Week07 Event Flow Smoke

## Goal

验证最小 Redis Streams 事件流是否已经具备以下链路：

- task create
- stream append
- consumer group create
- consume
- ack

## Local setup

### Redis

~~~bash
docker run -d --name week07-redis -p 6379:6379 redis:7-alpine
~~~

### PostgreSQL

~~~bash
docker run -d --name week07-postgres \
  -e POSTGRES_DB=media_task_db \
  -e POSTGRES_USER=media \
  -e POSTGRES_PASSWORD=media \
  -p 5432:5432 \
  postgres:16-alpine
~~~

### App

~~~bash
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/media_task_db \
SPRING_DATASOURCE_USERNAME=media \
SPRING_DATASOURCE_PASSWORD=media \
SPRING_REDIS_HOST=127.0.0.1 \
SPRING_REDIS_PORT=6379 \
./mvnw spring-boot:run
~~~

## Smoke steps

### Step 1: create one task

~~~bash
curl -sS \
  -X POST http://127.0.0.1:8080/api/media-tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"week07 smoke task","mediaType":"audio"}'
~~~

### Step 2: inspect stream

~~~bash
docker exec week07-redis redis-cli XRANGE media-task-events - + COUNT 10
~~~

### Step 3: first consume

~~~bash
curl -sS \
  -X POST http://127.0.0.1:8080/api/media-tasks/eventing/smoke-consume
~~~

Observed result in this round:

- first consume returned `NO_MESSAGE`

### Step 4: create one more task after group exists

~~~bash
curl -sS \
  -X POST http://127.0.0.1:8080/api/media-tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"week07 smoke task after group","mediaType":"audio"}'
~~~

### Step 5: consume again

~~~bash
curl -sS \
  -X POST http://127.0.0.1:8080/api/media-tasks/eventing/smoke-consume
~~~

Observed result in this round:

- returned `CONSUMED eventName=MediaTaskCreated ... acked=1`

### Step 6: check pending / group

~~~bash
docker exec week07-redis redis-cli XPENDING media-task-events media-task-group
docker exec week07-redis redis-cli XINFO GROUPS media-task-events
~~~

Observed result in this round:

- `XPENDING = 0`
- `consumers = 1`
- `entries-read = 2`
- `lag = 0`

## Current conclusion

What is verified:

- stream append works
- consumer group can be created
- new message after group creation can be consumed and acknowledged

What is not yet verified:

- backlog semantics for messages written before group creation
- full task status update loop
- retry / compensation / idempotency
