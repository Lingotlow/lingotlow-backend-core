package com.lingotlow.backendcore.infrastructure.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisStreamService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(String streamKey, Map<String, Object> message) {
        try {
            MapRecord<String, String, Object> record = StreamRecords.newRecord()
                    .in(streamKey)
                    .ofMap(message)
                    .withId(org.springframework.data.redis.connection.stream.RecordId.autoGenerate());

            org.springframework.data.redis.connection.stream.RecordId messageId = redisTemplate.opsForStream().add(record);
            log.debug("Message published to stream {} with id: {}", streamKey, messageId);
        } catch (Exception e) {
            log.error("Error publishing to Redis Stream: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish message to Redis", e);
        }
    }
}