package com.ryan.media.messaging;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class Consumer {

    public static final String GROUP = "media-task-group";
    public static final String CONSUMER_NAME = "week07-consumer";

    private final StringRedisTemplate stringRedisTemplate;

    public Consumer(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String streamKey() {
        return Producer.STREAM_KEY;
    }

    public String consumerGroup() {
        return GROUP;
    }

    public String consumerName() {
        return CONSUMER_NAME;
    }

    public String eventName() {
        return Producer.EVENT_NAME;
    }

    public boolean isReadyForWeek07Skeleton() {
        return stringRedisTemplate != null;
    }

    public void createGroupIfMissing() {
        try {
            stringRedisTemplate.opsForStream().createGroup(streamKey(), ReadOffset.latest(), GROUP);
        } catch (DataAccessException ex) {
            if (!containsBusyGroup(ex)) {
                throw ex;
            }
        }
    }

    private boolean containsBusyGroup(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("BUSYGROUP")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public List<MapRecord<String, Object, Object>> readOneBatch() {
        return stringRedisTemplate.opsForStream().read(
                org.springframework.data.redis.connection.stream.Consumer.from(GROUP, CONSUMER_NAME),
                StreamReadOptions.empty().count(1).block(Duration.ofSeconds(1)),
                StreamOffset.create(streamKey(), ReadOffset.lastConsumed())
        );
    }

    public long ack(String recordId) {
        return stringRedisTemplate.opsForStream().acknowledge(streamKey(), GROUP, recordId);
    }

    public String consumeOneForWeek07Smoke() {
        try {
            createGroupIfMissing();
            List<MapRecord<String, Object, Object>> messages = readOneBatch();
            if (messages == null || messages.isEmpty()) {
                return "NO_MESSAGE";
            }

            MapRecord<String, Object, Object> message = messages.get(0);
            long acked = ack(message.getId().getValue());

            Map<Object, Object> payload = message.getValue();
            Object eventName = payload.get("eventName");
            Object taskId = payload.get("taskId");

            return "CONSUMED eventName=" + eventName
                    + " taskId=" + taskId
                    + " recordId=" + message.getId().getValue()
                    + " acked=" + acked;
        } catch (Exception ex) {
            return "ERROR " + ex.getClass().getName() + " :: " + String.valueOf(ex.getMessage());
        }
    }
}
