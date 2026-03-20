package com.lingotlow.backendcore.integration;

import static org.assertj.core.api.Assertions.*;

import com.lingotlow.backendcore.infrastructure.queue.EventQueueProducer;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Redis Integration Tests")
class RedisIntegrationTest extends AbstractIntegrationTest {

  @Autowired private RedisTemplate<String, Object> redisTemplate;

  @Autowired private EventQueueProducer eventQueueProducer;

  @Test
  @DisplayName("Should connect to Redis and perform basic operations")
  void redisConnection_Success() {
    // Given
    String testKey = "test-key-" + UUID.randomUUID();
    String testValue = "test-value";

    // When
    redisTemplate.opsForValue().set(testKey, testValue);

    // Then
    String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);
    assertThat(retrievedValue).isEqualTo(testValue);

    // Cleanup
    redisTemplate.delete(testKey);
  }

  @Test
  @DisplayName("Should enqueue event using real Redis")
  void enqueueEvent_WithRealRedis_Success() throws Exception {
    // Given
    var message =
        new EventQueueProducer.EventQueueMessage(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "doc-123",
            "test.integration",
            "PENDING",
            Map.of("key", "value"));
    message.setHeaders(Map.of("tenantKey", "test-tenant"));

    // When
    boolean result = eventQueueProducer.enqueueEvent(message);

    // Then
    assertThat(result).isTrue();
  }
}
