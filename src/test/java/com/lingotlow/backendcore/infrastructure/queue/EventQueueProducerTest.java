package com.lingotlow.backendcore.infrastructure.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventQueueProducerTest {

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
    }

    @Test
    void enqueueEvent_Success() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        RecordId expectedRecordId = RecordId.of("1234567890-0");
        when(streamOperations.add(any())).thenReturn(expectedRecordId);

        // Act
        boolean result = eventQueueProducer.enqueueEvent(message);

        // Assert
        assertTrue(result);
        verify(streamOperations).add(any());
        verify(counter).increment();
    }

    @Test
    void enqueueEvent_Failure_ReturnsFalse() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        when(streamOperations.add(any())).thenReturn(null);

        // Act
        boolean result = eventQueueProducer.enqueueEvent(message);

        // Assert
        assertFalse(result);
        verify(streamOperations).add(any());
        verify(counter).increment(); // Rejected counter
    }

    @Test
    void enqueueEvent_ThrowsException_ReturnsFalse() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        when(streamOperations.add(any())).thenThrow(new RuntimeException("Redis connection failed"));

        // Act
        boolean result = eventQueueProducer.enqueueEvent(message);

        // Assert
        assertFalse(result);
        verify(streamOperations).add(any());
        verify(counter).increment(); // Rejected counter
    }

    @Test
    void enqueueEventWithRetry_SuccessOnFirstAttempt() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        RecordId expectedRecordId = RecordId.of("1234567890-0");
        when(streamOperations.add(any())).thenReturn(expectedRecordId);

        // Act
        boolean result = eventQueueProducer.enqueueEventWithRetry(message, 3);

        // Assert
        assertTrue(result);
        verify(streamOperations, times(1)).add(any());
        verify(counter, times(1)).increment(); // Success counter
    }

    @Test
    void enqueueEventWithRetry_SuccessAfterRetries() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        RecordId expectedRecordId = RecordId.of("1234567890-0");
        when(streamOperations.add(any()))
                .thenReturn(null) // First attempt fails
                .thenReturn(null) // Second attempt fails
                .thenReturn(expectedRecordId); // Third attempt succeeds

        // Act
        boolean result = eventQueueProducer.enqueueEventWithRetry(message, 3);

        // Assert
        assertTrue(result);
        verify(streamOperations, times(3)).add(any());
        verify(counter, times(1)).increment(); // Success counter
    }

    @Test
    void enqueueEventWithRetry_AllAttemptsFail() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        when(streamOperations.add(any())).thenReturn(null);

        // Act
        boolean result = eventQueueProducer.enqueueEventWithRetry(message, 3);

        // Assert
        assertFalse(result);
        verify(streamOperations, times(3)).add(any());
        verify(counter, times(1)).increment(); // Rejected counter
    }

    @Test
    void isQueueHealthy_ReturnsTrue() {
        // Arrange
        when(streamOperations.info(anyString())).thenReturn(mock(StreamInfo.XInfoStream.class));

        // Act
        boolean result = eventQueueProducer.isQueueHealthy();

        // Assert
        assertTrue(result);
        verify(streamOperations).info("events:stream");
    }

    @Test
    void isQueueHealthy_ReturnsFalseOnException() {
        // Arrange
        when(streamOperations.info(anyString())).thenThrow(new RuntimeException("Connection failed"));

        // Act
        boolean result = eventQueueProducer.isQueueHealthy();

        // Assert
        assertFalse(result);
        verify(streamOperations).info("events:stream");
    }

    @Test
    void enqueueEventWithIdempotencyCheck_FirstTime_Success() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        RecordId expectedRecordId = RecordId.of("1234567890-0");
        when(streamOperations.add(any())).thenReturn(expectedRecordId);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // Act
        boolean result = eventQueueProducer.enqueueEventWithIdempotencyCheck(message);

        // Assert
        assertTrue(result);
        verify(redisTemplate).hasKey("event:" + requestId);
        verify(streamOperations).add(any());
        verify(redisTemplate).opsForValue().set(eq("event:" + requestId), eq("processed"), eq(86400));
    }

    @Test
    void enqueueEventWithIdempotencyCheck_AlreadyProcessed_ReturnsTrue() {
        // Arrange
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                requestId, tenantId, "doc123", "test-event", "PENDING", new HashMap<>()
        );

        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // Act
        boolean result = eventQueueProducer.enqueueEventWithIdempotencyCheck(message);

        // Assert
        assertTrue(result);
        verify(redisTemplate).hasKey("event:" + requestId);
        verify(streamOperations, never()).add(any());
        verify(redisTemplate, never()).opsForValue().set(anyString(), anyString(), anyInt());
    }
}
