# media-task-platform-java

一个面向媒体任务管理场景的 Java / Spring Boot 最小平台骨架项目。

当前目标不是一次性做成完整平台，而是先建立可运行、可测试、可扩展的后端工程基线，为后续数据库、鉴权、可观测、异步任务流打底。

## Verified Scope

当前仓库已完成并留有证据的范围如下：

- 已完成 PostgreSQL JDBC 依赖接入与 Spring Boot JDBC 最小链路打通
- 已完成 PostgreSQL Testcontainers smoke test
- 已完成 Flyway 第一版 schema migration 落地（`src/main/resources/db/migration/V1__init.sql`）
- 已补数据库路线 ADR（`docs/adr/0001-db-choice.md`）
- 已补 RepositoryIT，能够覆盖数据库地基验证
- 已暴露 `/actuator/prometheus`，为 Cloud 侧抓取提供最小指标入口
- 已完成 Week06 最小认证路线 ADR（`docs/adr/0002-auth-strategy.md`）
- 已完成最小认证骨架：
  - `POST /auth/login` 为公开入口
  - `GET /auth/me` 为受保护入口
- 已补 `AuthIT`，验证：
  - login 公开访问可用
  - 未认证访问 `/auth/me` 返回 401
  - 认证态访问 `/auth/me` 可返回 principal 信息

一句话说，当前仓库已经从“数据库地基”推进到“数据库地基 + 最小认证壳 + 安全测试”阶段，Week06 的 auth 主线已经真正起盘。

## Not Yet Verified

以下内容仍未进入“已验证”范围，当前不能写满：

- 真正的 JWT 签发 / 解析 / 校验闭环
- 真实 media-task 业务接口切到受保护边界后的完整验证
- 更完整的角色模型与权限控制
- Redis / Kafka
- tracing / OTel
- Docker / Compose / Kubernetes 部署
- 更完整的错误响应约束与生产级安全配置

这些方向已经进入路线规划，但截至当前仓库状态，还不应写成“已完成”。

## Next Hard Milestone

下一阶段目标：

1. 把当前 auth skeleton 推进为真正可运行的最小 JWT 闭环
2. 至少选择一个现有 media-task API 切入受保护边界
3. 为认证相关路径补更完整的 integration test 与错误响应校验
4. 同步 README / 日志 / 测试证据，避免代码推进快于仓库叙事

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
    ├── controller
    │   ├── HealthController.java
    │   └── MediaTaskController.java
    ├── model
    │   ├── CreateMediaTaskRequest.java
    │   └── MediaTaskResponse.java
    └── service
        └── MediaTaskService.java

## Next Hard Milestone

下一阶段目标：

1. 固化 PostgreSQL 路线并引入 Flyway 做第一版 schema migration
2. 将内存版 `MediaTaskService` 替换为持久化实现
3. 增加 repository / service / controller 更明确分层
4. 为接口补齐 integration test 与错误响应约束
5. 为后续 Spring Security / JWT 留出清晰边界