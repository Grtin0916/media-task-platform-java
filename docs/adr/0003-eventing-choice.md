# ADR 0003 - Eventing Choice for Week07 Minimum Async Flow

## Status
Accepted

## Context
当前仓库已完成最小 auth skeleton，`POST /auth/login`、`GET /auth/me`、`GET/POST /api/media-tasks` 已进入受保护边界。
Week07 的目标不是建设完整 MQ 平台，而是先把“任务创建 -> 事件发布 -> 状态更新”做成最小异步链，并留下可引用证据。

## Decision
本周先选 Redis Streams，不选 Kafka。

## Why Redis Streams for Week07
1. 当前仓库还没有 MQ 基础设施，Redis Streams 的落地成本更低。
2. 本周目标是最小可运行异步链，不是消息平台化。
3. 后续如需更强的分区、副本、消费语义和平台治理，再迁到 Kafka。

## Why not Kafka this week
1. 本周时间窗口不适合引入 broker、topic、consumer group、运维脚本和更大的测试矩阵。
2. 会把“验证异步链存在”误做成“先建设消息平台”，投入结构失真。
3. 会拖慢 W8 阶段验收前的 README / 证据链收口。

## Minimum event definition
- eventName: `MediaTaskCreated`
- streamKey: `media-task-events`
- consumerGroup: `media-task-group`
- consumerName: `week07-consumer`

## Minimum payload
- taskId
- userId
- status
- createdAt
- traceId

## Minimum status transition
- request accepted -> `CREATED`
- event published -> `EVENT_PUBLISHED`
- consumer observed event -> `EVENT_CONSUMED`

## Scope for this week
- 补 Redis 依赖
- 落 `Producer.java`
- 落 `Consumer.java`
- 保持 skeleton 级别，不在今天强行写完整 happy path

## Deferred
- 完整 JWT 闭环
- 完整 event-flow integration test
- 幂等、重试、DLQ、补偿
- Kafka 迁移
