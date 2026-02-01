package com.lingotlow.backendcore.infrastructure.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class QueueIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    private EventQueueProducer eventQueueProducer;
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
        registry.add("QUEUE_URL", () -> "redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        eventQueueProducer = new EventQueueProducer(redisTemplate, objectMapper, meterRegistry);

        // Clear Redis before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @AfterEach
    void tearDown() {
        // Clear Redis after each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void enqueueEvent_RealRedis_Success() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        // Act
        boolean result = eventQueueProducer.enqueueEvent(message);

        // Assert
        assertTrue(result);
        assertTrue(eventQueueProducer.isQueueHealthy());
    }

    @Test
    void enqueueEventWithIdempotencyCheck_RealRedis_FirstTime_Success() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        // Act
        boolean result1 = eventQueueProducer.enqueueEventWithIdempotencyCheck(message);
        boolean result2 = eventQueueProducer.enqueueEventWithIdempotencyCheck(message);

        // Assert
        assertTrue(result1);
        assertTrue(result2); // Second call should succeed due to idempotency

        // Verify the idempotency key was set
        String idempotencyKey = "event:" + requestId;
        assertTrue(redisTemplate.hasKey(idempotencyKey));
    }

    @Test
    void enqueueEventWithRetry_RealRedis_SuccessOnFirstAttempt() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        // Act
        boolean result = eventQueueProducer.enqueueEventWithRetry(message, 3);

        // Assert
        assertTrue(result);
        assertTrue(eventQueueProducer.isQueueHealthy());
    }

    @Test
    void enqueueEvent_WithLargePayload_RealRedis_Success() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        Map<String, Object> largePayload = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            largePayload.put("key" + i, "value" + i);
        }

        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", largePayload
        );

        // Act
        boolean result = eventQueueProducer.enqueueEvent(message);

        // Assert
        assertTrue(result);
    }

    @Test
    void isQueueHealthy_RealRedis_ReturnsTrue() {
        // Act
        boolean result = eventQueueProducer.isQueueHealthy();

        // Assert
        assertTrue(result);
    }

    @Test
    void enqueueEvent_WithHeaders_RealRedis_Success() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        Map<String, String> headers = new HashMap<>();
        headers.put("tenantKey", "test-tenant");
        headers.put("userAgent", "test-agent");
        message.setHeaders(headers);

        // Act
        boolean result = eventQueueProducer.enqueueEvent(message);

        // Assert
        assertTrue(result);
    }

    @Test
    void enqueueEvent_WithNullPayload_RealRedis_Success() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", null
        );

        // Act
        boolean result = eventQueueProducer.enqueueEvent(message);

        // Assert
        assertTrue(result);
    }
}
