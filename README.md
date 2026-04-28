# media-task-platform-java

一个面向媒体任务管理场景的 Java / Spring Boot 最小平台骨架项目。

当前目标不是一次性做成完整平台，而是先建立可运行、可测试、可扩展的后端工程基线，为后续数据库、鉴权、可观测、异步任务流打底。

## Verified Scope

当前仓库已完成并留有证据的范围如下：

- 已完成 PostgreSQL + Flyway 的最小数据库地基
- 已完成媒体任务最小 CRUD 路径
- 已完成 Spring Security 最小认证壳
- 已完成 `POST /auth/login` 与 `GET /auth/me`
- 已将 `GET /api/media-tasks` 与 `POST /api/media-tasks` 切入受保护边界
- 已保留 `AuthIT` 与 Week06 认证相关日志证据
- 已新增 Redis 依赖（`spring-boot-starter-data-redis`）
- 已补 eventing 选型 ADR（`docs/adr/0003-eventing-choice.md`）
- 已新增 `messaging/Producer.java`，定义 `MediaTaskCreated` 事件与最小 stream append 骨架
- 已新增 `messaging/Consumer.java`，固定 stream key / consumer group / consumer name，并保留 Week07 TODO

- 已完成 1 次本地 Redis Streams event-flow smoke，保留了 create / XRANGE / consume / ack 相关日志证据

- 已验证在 consumer group 存在后，新进入 stream 的 `MediaTaskCreated` 事件可被 consume 并 ack

一句话说，当前仓库已经从“数据库地基”推进到“数据库地基 + 最小认证壳 + 安全测试”阶段，并在 Week07 开始进入最小 eventing skeleton 阶段；当前仓库已经不再只是认证壳，而是开始具备“任务创建 -> 事件发布 -> consume / ack”的最小异步链路入口。

- 已完成 Week08 DB EXPLAIN baseline：`docs/benchmarks/db_explain_week08.md` 已记录 Query A / Query B / Query C 三类查询形态、索引选择、Planning Time、Execution Time 与证据边界。
- 已完成 Week08 索引迁移：`src/main/resources/db/migration/V2__indexes.sql` 新增 `idx_media_task_created_at_desc`、`idx_media_task_status_created_at_desc`、`idx_media_asset_task_id_created_at_desc`，并明确每个索引必须绑定具体 EXPLAIN-backed query。
- 已完成 Week08 `QueryPlanIT` 集成测试：`src/test/java/com/ryan/media/QueryPlanIT.java` 可通过 Testcontainers PostgreSQL 16-alpine seed 数据、执行 `ANALYZE`、输出三组 `EXPLAIN ANALYZE` 日志。
- 已完成 2026-04-28 本地复验：`artifacts/logs/week08_db_query_plan_it_rerun_20260428.log`、`week08_db_explain_summary_rerun_20260428.log` 与三份 `*_rerun_20260428.log` 已保留。当前 Query A 与 Query C 命中预期索引；Query B 使用 `idx_media_task_created_at_desc` 并通过 status filter，不声明 `idx_media_task_status_created_at_desc` 在当前 seed 分布下被选中。

- 已完成 2026-04-29 周三复验：`QueryPlanIT` 重新通过，`artifacts/logs/week08_db_query_plan_it_rerun_20260429.log` 与 `artifacts/logs/week08_db_explain_summary_rerun_20260429.log` 已保留；Query A 继续命中 `idx_media_task_created_at_desc`，Query C 继续命中 `idx_media_asset_task_id_created_at_desc`，Query B 仍使用 `idx_media_task_created_at_desc` 并执行 status filter，不声明 `idx_media_task_status_created_at_desc` 在当前 seed 分布下被 planner 选中。

## Not Yet Verified

以下内容仍未进入“已验证”范围，当前不能写满：

- Redis Streams 已完成最小本地 smoke，当前已验证 append / consume / ack；但建组前 backlog 的消费语义与任务状态更新闭环仍未完成
- 完整 JWT 签发 / 解析 / 校验闭环
- 更完整的任务状态机、幂等、重试、补偿与失败恢复
- Kafka 或更重消息平台的落地与对比
- 更系统的 observability / tracing / load test 闭环

这些方向已经进入路线规划，但截至当前仓库状态，还不应写成“已完成”。

- Week08 DB baseline 只验证了本地 Testcontainers PostgreSQL 16-alpine 下的固定 seed 查询，不代表生产性能提升。
- Query B 当前没有选择 `idx_media_task_status_created_at_desc`，后续需要在更高 status selectivity、更深分页或更真实数据分布下重新评估。
- 当前还没有完成正式分页 / 排序 API contract，也没有把 SQL plan 指标接入 Actuator / Prometheus。

## Next Hard Milestone

1. Week08：收口 SQL explain 证据链
   * 固定 `V2__indexes.sql`、`QueryPlanIT` 与 `docs/benchmarks/db_explain_week08.md`
   * 保留 2026-04-28 rerun 证据日志
   * 明确 Query A / Query C 是当前可引用索引命中证据，Query B 只作为“优化器选择与数据分布相关”的边界案例

2. Week09：转入 Java observability / JVM 可观测预热
   * 将 Actuator metrics、SQL query evidence 与后续 Java runtime 指标衔接
   * 不继续无依据堆索引

## Tech Stack

- Java 21
- Spring Boot 3.5.13
- Spring Web
- Spring Boot Actuator
- Spring Validation
- JUnit 5
- MockMvc
- Maven Wrapper

## Local Run

    ./mvnw test
    ./mvnw spring-boot:run

## Quick Verify

    curl -s http://127.0.0.1:8080/health
    curl -s http://127.0.0.1:8080/actuator/health
    curl -s -X POST http://127.0.0.1:8080/api/media-tasks \
      -H "Content-Type: application/json" \
      -d '{"title":"first-task","mediaType":"audio"}'
    curl -s http://127.0.0.1:8080/api/media-tasks

## Project Structure

    src/main/java/com/ryan/media/
    ├── MediaTaskApplication.java
    ├── auth
    │   └── AuthController.java
    ├── controller
    │   ├── HealthController.java
    │   └── MediaTaskController.java
    ├── model
    │   ├── CreateMediaTaskRequest.java
    │   └── MediaTaskResponse.java
    ├── repository
    │   └── MediaTaskRepository.java
    ├── security
    │   └── SecurityConfig.java
    └── service
        └── MediaTaskService.java

