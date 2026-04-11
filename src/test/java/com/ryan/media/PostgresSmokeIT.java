package com.ryan.media;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

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
class PostgresSmokeIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void should_connect_to_real_postgres_container() throws Exception {
        assertThat(dataSource).isNotNull();
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.isValid(2)).isTrue();
        }
    }

    @Test
    void should_execute_simple_sql_against_postgres() {
        Integer one = jdbcClient.sql("select 1").query(Integer.class).single();
        assertThat(one).isEqualTo(1);
    }

    @Test
    void should_run_flyway_migration_and_create_core_tables() {
        Integer mediaTaskTable =
                jdbcClient.sql("""
                        select count(*)
                        from information_schema.tables
                        where table_schema = 'public'
                          and table_name = 'media_task'
                        """).query(Integer.class).single();

        Integer mediaAssetTable =
                jdbcClient.sql("""
                        select count(*)
                        from information_schema.tables
                        where table_schema = 'public'
                          and table_name = 'media_asset'
                        """).query(Integer.class).single();

        Integer mediaTagTable =
                jdbcClient.sql("""
                        select count(*)
                        from information_schema.tables
                        where table_schema = 'public'
                          and table_name = 'media_tag'
                        """).query(Integer.class).single();

        assertThat(mediaTaskTable).isEqualTo(1);
        assertThat(mediaAssetTable).isEqualTo(1);
        assertThat(mediaTagTable).isEqualTo(1);
    }
}
