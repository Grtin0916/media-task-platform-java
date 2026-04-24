# 2026-04-24 Week07 Java Eventing Summary

## 1. Goal

本次收口目标不是继续扩展消息系统，而是把 Week07 已完成的 Redis Streams event-flow smoke 固化成可直接引用的证据入口。

本次核查围绕以下问题展开：

- README 是否已经同步 Week07 eventing verified scope
- `docs/adr/0003-eventing-choice.md` 是否解释了为什么本周先选 Redis Streams
- `Producer.java` 是否已经定义 `MediaTaskCreated` 与 stream append 骨架
- `Consumer.java` 是否已经固定 stream key / group / consumer name，并具备 consume / ack smoke 入口
- `loadtest/event-flow-smoke.md` 是否记录 create / XRANGE / consume / ack 的可复现步骤
- artifacts 日志是否能支撑 README 中的已验证结论

## 2. Current verified status

当前 Java 仓库已经完成以下 Week07 资产：

- 已新增 Redis 依赖
- 已补 eventing 选型 ADR：`docs/adr/0003-eventing-choice.md`
- 已新增 `src/main/java/com/ryan/media/messaging/Producer.java`
- 已新增 `src/main/java/com/ryan/media/messaging/Consumer.java`
- 已补 `loadtest/event-flow-smoke.md`
- 已完成一次本地 Redis Streams event-flow smoke
- 已保留 create / XRANGE / consume / ack / XPENDING / XINFO GROUPS 相关日志证据

当前最小链路已经覆盖：

- task create
- stream append
- consumer group create
- consume
- ack
- pending check
- group info check

## 3. Eventing constants

本轮 Week07 eventing skeleton 固定以下命名：

- event name: `MediaTaskCreated`
- stream key: `media-task-events`
- consumer group: `media-task-group`
- consumer name: `week07-consumer`

这些命名已经同时出现在 ADR、Producer、Consumer、loadtest 文档与日志证据中。

## 4. Evidence checked

本次收口涉及以下文件：

- `README.md`
- `docs/adr/0003-eventing-choice.md`
- `loadtest/event-flow-smoke.md`
- `src/main/java/com/ryan/media/messaging/Producer.java`
- `src/main/java/com/ryan/media/messaging/Consumer.java`
- `artifacts/logs/week07_event_flow_app_local.log`
- `artifacts/logs/week07_event_flow_smoke_step2.json`
- `artifacts/logs/week07_event_flow_xinfo_groups.log`
- `artifacts/logs/week07_event_flow_xpending.log`
- `artifacts/logs/week07_event_flow_xrange_after_consume.log`
- `artifacts/logs/week07_event_flow_xrange_after_step3.log`

## 5. What is verified

当前可以写入 verified scope 的内容是：

- Redis Streams append 路径存在
- consumer group 可以创建
- consumer group 存在后，新进入 stream 的 `MediaTaskCreated` 可以被消费
- 已消费消息可以 ack
- `XPENDING` 可以作为 smoke 后的 pending check
- `XINFO GROUPS` 可以作为 group 状态检查入口

这些结论只代表 Week07 本地最小 smoke，不代表完整消息平台已经完成。

## 6. Not yet verified

以下内容仍不能写成已完成：

- 建组前已经写入 stream 的 backlog 消费语义
- 任务状态更新闭环
- consumer 失败后的重试策略
- 幂等消费
- DLQ / 补偿机制
- Kafka 对比与迁移
- event-flow integration test
- observability / tracing / load test 闭环

这些内容进入 W8 / W9 / W11 后续路线，不在 Week07 强行写满。

## 7. Decision

Java Week07 当前状态判定为：

- eventing ADR：完成
- Producer skeleton：完成
- Consumer skeleton：完成
- event-flow smoke 文档：完成
- 本地 Redis Streams smoke：完成
- README verified / not yet verified 边界：完成
- Week07 weekly 收口入口：本文件补齐

本次收口后，Java 仓库不继续扩展 Kafka、DLQ、重试、幂等或完整状态机。下一步优先转向 Cloud CI evidence 收口。

## 8. Next hard milestone

W8 Java 下一硬里程碑建议聚焦数据库与事件语义交界处：

- 明确 consumer group 首次偏移策略
- 评估 backlog 消费语义
- 明确任务状态更新责任边界
- 开始 SQL tuning 与 `EXPLAIN / ANALYZE` 证据链
- 避免把消息系统扩展成脱离当前仓库阶段的“大而空”平台
