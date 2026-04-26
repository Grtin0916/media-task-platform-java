package com.ryan.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = MediaTaskApplication.class)
@Testcontainers
class QueryPlanIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void should_write_week08_explain_analyze_logs() throws Exception {
        seedData();

        Path logDir = Path.of("artifacts/logs");
        Files.createDirectories(logDir);

        String queryA = explain("""
                select id, title, media_type, status, created_at
                from media_task
                order by created_at desc
                limit 20
                """);

        String queryB = explain("""
                select id, title, media_type, status, created_at
                from media_task
                where status = 'CREATED'
                order by created_at desc
                limit 20
                """);

        String queryC = explain("""
                select id, task_id, asset_path, asset_kind, created_at
                from media_asset
                where task_id = 'task-000001'
                order by created_at desc
                limit 20
                """);

        Files.writeString(logDir.resolve("week08_db_explain_query_a_recent_tasks.log"), queryA);
        Files.writeString(logDir.resolve("week08_db_explain_query_b_status_recent_tasks.log"), queryB);
        Files.writeString(logDir.resolve("week08_db_explain_query_c_task_assets.log"), queryC);

        assertThat(queryA).contains("Planning Time").contains("Execution Time");
        assertThat(queryB).contains("Planning Time").contains("Execution Time");
        assertThat(queryC).contains("Planning Time").contains("Execution Time");
    }

    private void seedData() {
        jdbcClient.sql("delete from media_tag").update();
        jdbcClient.sql("delete from media_asset").update();
        jdbcClient.sql("delete from media_task").update();

        jdbcClient.sql("""
                insert into media_task (
                    id,
                    title,
                    media_type,
                    status,
                    created_at,
                    updated_at
                )
                select
                    'task-' || lpad(gs::text, 6, '0') as id,
                    'week08 seed task ' || gs as title,
                    case when gs % 2 = 0 then 'video' else 'audio' end as media_type,
                    case
                        when gs % 3 = 0 then 'CREATED'
                        when gs % 3 = 1 then 'RUNNING'
                        else 'FAILED'
                    end as status,
                    now() - (gs || ' minutes')::interval as created_at,
                    now() - (gs || ' minutes')::interval as updated_at
                from generate_series(1, 5000) as gs
                """).update();

        jdbcClient.sql("""
                insert into media_asset (
                    id,
                    task_id,
                    asset_path,
                    asset_kind,
                    created_at
                )
                select
                    'asset-' || lpad(gs::text, 6, '0') as id,
                    'task-000001' as task_id,
                    '/tmp/week08/asset-' || gs || '.wav' as asset_path,
                    case when gs % 2 = 0 then 'audio' else 'preview' end as asset_kind,
                    now() - (gs || ' seconds')::interval as created_at
                from generate_series(1, 300) as gs
                """).update();

        jdbcClient.sql("analyze media_task").update();
        jdbcClient.sql("analyze media_asset").update();
    }

    private String explain(String sql) {
        List<String> rows = jdbcClient.sql("explain analyze " + sql)
                .query(String.class)
                .list();

        return String.join(System.lineSeparator(), rows) + System.lineSeparator();
    }
}
