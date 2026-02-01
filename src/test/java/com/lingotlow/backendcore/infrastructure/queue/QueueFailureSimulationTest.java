package com.lingotlow.backendcore.infrastructure.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueFailureSimulationTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private io.micrometer.core.instrument.Counter counter;

    private EventQueueProducer eventQueueProducer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        eventQueueProducer = new EventQueueProducer(redisTemplate, objectMapper, meterRegistry);

        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(meterRegistry.counter(anyString())).thenReturn(counter);
    }

    @Test
    void simulateRedisConnectionFailure() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        when(streamOperations.add(any()))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        // Act
        boolean result = eventQueueProducer.enqueueEvent(message);

        // Assert
        assertFalse(result);
        verify(streamOperations).add(any());
        verify(counter).increment(); // Rejected counter
    }

    @Test
    void simulateRedisTimeout() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        when(streamOperations.add(any()))
                .thenThrow(new RuntimeException("Redis timeout after 2000ms"));

        // Act
        boolean result = eventQueueProducer.enqueueEvent(message);

        // Assert
        assertFalse(result);
        verify(streamOperations).add(any());
        verify(counter).increment(); // Rejected counter
    }

    @Test
    void simulateSerializationFailure() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        Map<String, Object> payload = new HashMap<>();
        payload.put("data", new Object()); // Object that might cause serialization issues

        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", payload
        );

        when(streamOperations.add(any()))
                .thenThrow(new RuntimeException("JSON serialization failed"));

        // Act
        boolean result = eventQueueProducer.enqueueEvent(message);

        // Assert
        assertFalse(result);
        verify(streamOperations).add(any());
        verify(counter).increment(); // Rejected counter
    }

    @Test
    void simulatePartialFailure_RetrySuccess() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        RecordId expectedRecordId = RecordId.of("1234567890-0");
        when(streamOperations.add(any()))
                .thenThrow(new RuntimeException("Temporary network failure")) // First attempt fails
                .thenReturn(expectedRecordId); // Second attempt succeeds

        // Act
        boolean result = eventQueueProducer.enqueueEventWithRetry(message, 3);

        // Assert
        assertTrue(result);
        verify(streamOperations, times(2)).add(any());
        verify(counter, times(1)).increment(); // Success counter
    }

    @Test
    void simulateQueueFull_Rejected() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        when(streamOperations.add(any()))
                .thenThrow(new RuntimeException("Queue is full"));

        // Act
        boolean result = eventQueueProducer.enqueueEventWithRetry(message, 3);

        // Assert
        assertFalse(result);
        verify(streamOperations, times(3)).add(any());
        verify(counter, times(1)).increment(); // Rejected counter
    }

    @Test
    void simulateHealthCheck_Failure() {
        // Arrange
        when(streamOperations.info(anyString()))
                .thenThrow(new RedisConnectionFailureException("Cannot connect to Redis"));

        // Act
        boolean result = eventQueueProducer.isQueueHealthy();

        // Assert
        assertFalse(result);
        verify(streamOperations).info("events:stream");
    }

    @Test
    void simulateIdempotencyCheck_RedisFailure() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        when(redisTemplate.hasKey(anyString()))
                .thenThrow(new RuntimeException("Redis connection failed"));

        // Act
        boolean result = eventQueueProducer.enqueueEventWithIdempotencyCheck(message);

        // Assert
        assertFalse(result);
        verify(redisTemplate).hasKey("event:" + requestId);
        verify(streamOperations, never()).add(any());
    }

    @Test
    void simulateIdempotencyCheck_SetKeyFailure() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        RecordId expectedRecordId = RecordId.of("1234567890-0");
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(streamOperations.add(any())).thenReturn(expectedRecordId);
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Failed to set key"));

        // Act
        boolean result = eventQueueProducer.enqueueEventWithIdempotencyCheck(message);

        // Assert
        assertFalse(result);
        verify(redisTemplate).hasKey("event:" + requestId);
        verify(streamOperations).add(any());
        verify(redisTemplate).opsForValue();
    }

    @Test
    void simulateConcurrentAccess_IdempotencyHandling() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message1 = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );
        EventQueueProducer.EventQueueMessage message2 = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        RecordId expectedRecordId = RecordId.of("1234567890-0");

        // First call: key doesn't exist, enqueue successfully
        when(redisTemplate.hasKey("event:" + requestId)).thenReturn(false, true); // Second call returns true
        when(streamOperations.add(any())).thenReturn(expectedRecordId);

        // Act
        boolean result1 = eventQueueProducer.enqueueEventWithIdempotencyCheck(message1);
        boolean result2 = eventQueueProducer.enqueueEventWithIdempotencyCheck(message2);

        // Assert
        assertTrue(result1);
        assertTrue(result2); // Second call should return true due to idempotency
        verify(redisTemplate, times(2)).hasKey("event:" + requestId);
        verify(streamOperations, times(1)).add(any()); // Only one actual enqueue
    }

    @Test
    void simulateMemoryPressure_LargePayload() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        // Create a large payload that might cause memory issues
        Map<String, Object> largePayload = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            largePayload.put("key" + i, "value" + i);
        }

        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", largePayload
        );

        when(streamOperations.add(any()))
                .thenThrow(new OutOfMemoryError("Java heap space"));

        // Act
        boolean result = eventQueueProducer.enqueueEvent(message);

        // Assert
        assertFalse(result);
        verify(streamOperations).add(any());
        verify(counter).increment(); // Rejected counter
    }
}
