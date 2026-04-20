package com.ryan.media.messaging;

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

    // TODO(week07): create group if missing, read one batch, then ack after processing.
}
