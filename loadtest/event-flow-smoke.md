# Week07 Event Flow Smoke

## 1. Goal

验证 Java 仓在 Week07 是否已经具备最小异步事件链的真实本地证据：

- task create
- event publish
- event consume
- ack

本次目标不是完成完整消息平台，不补 JWT 闭环，不补状态更新持久化，只确认最小 event flow 已存在且可运行。

## 2. Environment

### 2.1 Redis
- container: `week07-redis`
- image: `redis:7-alpine`
- port: `6379`

### 2.2 PostgreSQL
- container: `week07-postgres`
- image: `postgres:16-alpine`
- db: `media_task_db`
- user: `media`
- port: `5432`

### 2.3 Spring Boot app
启动方式：

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/media_task_db \
SPRING_DATASOURCE_USERNAME=media \
SPRING_DATASOURCE_PASSWORD=media \
JAVA_TOOL_OPTIONS= \
./mvnw spring-boot:run
```

## 3. Current Week07 implementation boundary

当前仓库已具备：

- `Producer.publishMediaTaskCreated(...)`
- `MediaTaskService.create(...)` 在保存任务后触发 event publish
- `Consumer.consumeOneForWeek07Smoke()` 支持 group create-if-missing、read one batch、ack
- `POST /api/media-tasks/eventing/smoke-consume` 作为本地 smoke 入口

当前仍未纳入本次 smoke 范围：

- 真正可用的 JWT issuing
- consumer 后的数据库状态更新
- 幂等、重试、DLQ、补偿
- Kafka 迁移

## 4. Smoke commands

### 4.1 create task
```bash
curl -i -X POST http://127.0.0.1:8080/api/media-tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"week07-eventing-smoke-3","mediaType":"audio"}'
```

### 4.2 consume one event
```bash
curl -i -X POST http://127.0.0.1:8080/api/media-tasks/eventing/smoke-consume
```

## 5. Observed result

### 5.1 create result
- HTTP status: `201`
- task id: `5aa858fc-89f8-4c87-a8c2-07b49ed3481f`
- task status: `CREATED`

### 5.2 consume result
- HTTP status: `200`
- response code: `EVENTING_SMOKE_RESULT`
- result:
  - `CONSUMED eventName=MediaTaskCreated taskId=8a18eeba-b17c-423e-ad5e-2aeccfb8cb82 recordId=1776788082827-0 acked=1`

## 6. Interpretation

本次 smoke 证明以下事实已经成立：

1. `POST /api/media-tasks` 已能真实写入数据库并返回 `201`
2. 任务创建后，`MediaTaskCreated` 事件已进入 Redis Stream
3. consumer group 能真实读取一条消息
4. 读取后能够完成 `ack=1`

需要注意的是，本次消费到的不是最新创建的 `5aa858fc-89f8-4c87-a8c2-07b49ed3481f`，而是较早进入 stream 的 `8a18eeba-b17c-423e-ad5e-2aeccfb8cb82`。这说明当前 group 在按 backlog 顺序消费；对于 Week07 smoke 来说，这是可接受现象，不构成失败。

## 7. Conclusion

Week07 的 Java 最小 event flow 已经具备本地可引用证据：

- create -> publish -> consume -> ack

当前可以把仓库状态从“eventing skeleton”推进到“eventing smoke verified”。

## 8. Next step

下一步优先级如下：

1. README 顶部三段同步到 eventing smoke verified
2. 视需要补一次 `list + consume` 的附加证据
3. 收口本次 Week07 eventing 提交
