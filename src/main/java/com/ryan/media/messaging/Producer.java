package com.ryan.media.messaging;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class Producer {

    public static final String STREAM_KEY = "media-task-events";
    public static final String EVENT_NAME = "MediaTaskCreated";

    private final StringRedisTemplate stringRedisTemplate;

    public Producer(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public RecordId publishMediaTaskCreated(String taskId, String userId, String status, String traceId) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("eventName", EVENT_NAME);
        payload.put("taskId", taskId);
        payload.put("userId", userId);
        payload.put("status", status);
        payload.put("createdAt", Instant.now().toString());
        payload.put("traceId", traceId);

        var record = StreamRecords.string(payload).withStreamKey(STREAM_KEY);
        return stringRedisTemplate.opsForStream().add(record);
    }
}
