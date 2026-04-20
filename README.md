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

一句话说，当前仓库已经从“数据库地基”推进到“数据库地基 + 最小认证壳 + 安全测试”阶段，并在 Week07 开始进入最小 eventing skeleton 阶段；当前仓库已经不再只是认证壳，而是开始具备“任务创建 -> 事件发布 -> 状态更新”的异步链路入口。

## Not Yet Verified

以下内容仍未进入“已验证”范围，当前不能写满：

- Redis Streams 当前仅到 ADR + Producer / Consumer skeleton，尚未完成本地 event flow smoke、group 创建、read/ack 与状态更新闭环
- 完整 JWT 签发 / 解析 / 校验闭环
- 更完整的任务状态机、幂等、重试、补偿与失败恢复
- Kafka 或更重消息平台的落地与对比
- 更系统的 observability / tracing / load test 闭环

这些方向已经进入路线规划，但截至当前仓库状态，还不应写成“已完成”。

## Next Hard Milestone

接下来的硬里程碑按顺序是：

1. Week07：最小 eventing skeleton 落盘
   - 固定 Redis Streams 选型与事件字段
   - 保留 `Producer.java` / `Consumer.java` 最小骨架
   - 为后续“任务创建 -> 事件发布 -> 状态更新”异步链做入口
2. Week07：补最小 event-flow smoke
   - 至少完成 1 次本地 Redis append / consume 验证
   - 留下日志或文档证据
3. W8 阶段验收预热
   - 同步 README / 测试 / ADR / 日志证据
   - 为后续 SQL tuning、eventing 深化与 observability 链接入口

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

