package com.ryan.media.repository;

import com.ryan.media.model.MediaTaskResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class MediaTaskRepository {

    private final JdbcClient jdbcClient;

    public MediaTaskRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void save(MediaTaskResponse task) {
        int rows = jdbcClient.sql("""
                insert into media_task (
                    id,
                    title,
                    media_type,
                    status,
                    created_at,
                    updated_at
                ) values (
                    :id,
                    :title,
                    :mediaType,
                    :status,
                    :createdAt,
                    :updatedAt
                )
                """)
                .param("id", task.id())
                .param("title", task.title())
                .param("mediaType", task.mediaType())
                .param("status", task.status())
                .param("createdAt", Timestamp.from(task.createdAt()))
                .param("updatedAt", Timestamp.from(task.createdAt()))
                .update();

        if (rows != 1) {
            throw new IllegalStateException("expected 1 inserted row, but got: " + rows);
        }
    }

    public List<MediaTaskResponse> findAll() {
        return jdbcClient.sql("""
                select id, title, media_type, status, created_at
                from media_task
                order by created_at desc
                """)
                .query((rs, rowNum) -> new MediaTaskResponse(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("media_type"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant()
                ))
                .list();
    }

    public Optional<MediaTaskResponse> findById(String id) {
        return jdbcClient.sql("""
                select id, title, media_type, status, created_at
                from media_task
                where id = :id
                """)
                .param("id", id)
                .query((rs, rowNum) -> new MediaTaskResponse(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("media_type"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant()
                ))
                .optional();
    }

    public int deleteById(String id) {
        return jdbcClient.sql("""
                delete from media_task
                where id = :id
                """)
                .param("id", id)
                .update();
    }
}
