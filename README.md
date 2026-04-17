# media-task-platform-java

一个面向媒体任务管理场景的 Java / Spring Boot 最小平台骨架项目。

当前目标不是一次性做成完整平台，而是先建立可运行、可测试、可扩展的后端工程基线，为后续数据库、鉴权、可观测、异步任务流打底。

## Verified Scope

当前仓库已完成并留有证据的范围如下：
- 已引入 Spring Security 并形成最小 auth skeleton
- `POST /auth/login` 为公开入口
- `GET /auth/me` 为受保护入口
- `GET /api/media-tasks` 已进入受保护边界
- `POST /api/media-tasks` 已进入受保护边界
- 未认证访问 `/auth/me` 返回 401
- 认证态访问 `/auth/me` 返回 200
- 未认证访问 `GET /api/media-tasks` 返回 401
- 认证态访问 `GET /api/media-tasks` 返回 200
- 未认证访问 `POST /api/media-tasks` 返回 401
- 认证态访问 `POST /api/media-tasks` 时，当前非法 payload 会进入业务校验并返回 400
- 已补 `src/test/java/com/ryan/media/AuthIT.java`
- 已保留 `artifacts/logs/week06_authit_004.log` 作为 Week06 最小认证测试证据

一句话说，当前仓库已经不只是“安全边界骨架存在”，而是已经用 AuthIT 把公开入口、受保护入口，以及一个写接口的最小认证行为测实。

## Not Yet Verified

以下内容仍未进入“已验证”范围，当前不能写满：

- 真正的 JWT 签发 / 解析 / 校验闭环
- 基于角色的 403 区分与更细粒度授权
- `GET /api/media-tasks/{id}`、`DELETE /api/media-tasks/{id}` 等更多接口切入受保护边界后的完整验证
- 认证态下合法 `POST /api/media-tasks` 请求返回 201 的完整 happy path
- 更统一的认证失败 / 校验失败错误响应格式

## Next Hard Milestone

接下来的硬里程碑按顺序是：

1. Week06 收口：README / ADR / AuthIT / 日志证据同步
   - 确保 README、`0002-auth-strategy.md`、`AuthIT.java`、`week06_authit_004.log` 一致
   - 把当前最小认证证据固定成可引用资产

2. 随后：把 auth skeleton 推进为真正可运行的最小 JWT 闭环
   - 补 token issuing / parsing / validation
   - 再把更多 media-task 业务接口推进到受保护边界

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

