# media-task-platform-java

一个面向媒体任务管理场景的 Java / Spring Boot 最小平台骨架项目。

当前目标不是一次性做成完整平台，而是先建立可运行、可测试、可扩展的后端工程基线，为后续数据库、鉴权、可观测、异步任务流打底。

## Verified Scope


- 已完成 PostgreSQL JDBC 依赖接入与 Spring Boot JDBC 最小链路打通
- 已完成 PostgreSQL Testcontainers smoke test：可拉起真实 PostgreSQL 容器、建立 DataSource 连接并执行最小 SQL 查询
当前已验证能力：

- Java 21 + Maven Wrapper 环境可用
- Spring Boot 3.5.13 应用可启动
- 自定义健康检查接口：`GET /health`
- Actuator 健康检查接口：`GET /actuator/health`
- 最小媒体任务 CRUD：
  - `POST /api/media-tasks`
  - `GET /api/media-tasks`
  - `GET /api/media-tasks/{id}`
  - `DELETE /api/media-tasks/{id}`
- `./mvnw test` 已通过
- 当前任务存储方式为内存存储（`ConcurrentHashMap`）

## Not Yet Verified


- 正式数据库 schema 设计与持久化落地
当前尚未验证或尚未接入：

- Flyway / Liquibase 迁移
- JPA / MyBatis
- Spring Security / JWT
- Redis / Kafka
- Prometheus / Micrometer / tracing
- Docker / Compose / Kubernetes 部署

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