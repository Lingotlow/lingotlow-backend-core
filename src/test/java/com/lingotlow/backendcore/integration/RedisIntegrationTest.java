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
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.redis.host=localhost",
    "spring.redis.port=6379",
    "spring.redis.timeout=2000ms"
})
@DisplayName("Redis Integration Tests")
class RedisIntegrationTest {

  @Autowired private RedisTemplate<String, Object> redisTemplate;

  @Autowired private EventQueueProducer eventQueueProducer;

  @Test
  @DisplayName("Should connect to Redis and perform basic operations")
  void redisConnection_Success() {
    // Given
    String testKey = "test-key-" + UUID.randomUUID();
    String testValue = "test-value";

    try {
      // When
      redisTemplate.opsForValue().set(testKey, testValue);

      // Then
      String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);
      assertThat(retrievedValue).isEqualTo(testValue);

      // Cleanup
      redisTemplate.delete(testKey);
    } catch (Exception e) {
      // Skip test if Redis is not available
      org.junit.jupiter.api.Assumptions.assumeTrue(false, "Redis is not available for integration testing: " + e.getMessage());
    }
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

    try {
      // When
      boolean result = eventQueueProducer.enqueueEvent(message);

      // Then
      assertThat(result).isTrue();
    } catch (Exception e) {
      // Skip test if Redis is not available
      org.junit.jupiter.api.Assumptions.assumeTrue(false, "Redis is not available for integration testing: " + e.getMessage());
    }
  }
}
