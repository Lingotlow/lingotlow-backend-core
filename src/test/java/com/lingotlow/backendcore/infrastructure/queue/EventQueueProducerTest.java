package com.lingotlow.backendcore.infrastructure.queue;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventQueueProducer Unit Tests")
class EventQueueProducerTest {

  @Mock private RedisTemplate<String, Object> redisTemplate;

  @Mock private ObjectMapper objectMapper;

  @Mock private MeterRegistry meterRegistry;

  @Mock private StreamOperations<String, Object, Object> streamOperations;

  @Mock private Counter rejectedCounter;

  @Mock private Counter processedCounter;

  private EventQueueProducer eventQueueProducer;
  private EventQueueProducer.EventQueueMessage sampleMessage;
  private MockedStatic<Counter> counterMock;
  private org.springframework.data.redis.core.ValueOperations<String, Object> valueOperations;

  @BeforeEach
  void setUp() {
    eventQueueProducer = new EventQueueProducer(redisTemplate, objectMapper, meterRegistry);
    lenient().when(redisTemplate.opsForStream()).thenReturn(streamOperations);

    // Mock Counter.builder.register() pattern
    lenient().doNothing().when(processedCounter).increment();
    lenient().doNothing().when(rejectedCounter).increment();

    // Mock Counter.builder static method
    counterMock = mockStatic(Counter.class);
    Counter.Builder enqueuedBuilder = mock(Counter.Builder.class);
    Counter.Builder rejectedBuilder = mock(Counter.Builder.class);
    lenient().when(enqueuedBuilder.description(anyString())).thenReturn(enqueuedBuilder);
    lenient().when(rejectedBuilder.description(anyString())).thenReturn(rejectedBuilder);
    lenient().when(enqueuedBuilder.register(meterRegistry)).thenReturn(processedCounter);
    lenient().when(rejectedBuilder.register(meterRegistry)).thenReturn(rejectedCounter);
    when(Counter.builder(anyString())).thenReturn(enqueuedBuilder, rejectedBuilder);

    // Mock redisTemplate.opsForValue() for idempotency tests
    valueOperations = mock(org.springframework.data.redis.core.ValueOperations.class);
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    lenient().doNothing().when(valueOperations).set(anyString(), anyString(), anyLong());

    sampleMessage =
        new EventQueueProducer.EventQueueMessage(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "doc-123",
            "test.event",
            "PENDING",
            Map.of("key", "value"));
    sampleMessage.setHeaders(Map.of("tenantKey", "test-tenant"));
  }

  @AfterEach
  void tearDown() {
    if (counterMock != null) {
      counterMock.close();
    }
  }

  @Test
  @DisplayName("Should enqueue event successfully")
  void enqueueEvent_Success() throws Exception {
    // Given
    RecordId expectedRecordId = RecordId.of("1234567890-0");
    when(streamOperations.add(any(StringRecord.class))).thenReturn(expectedRecordId);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"key\":\"value\"}");

    // When
    boolean result = eventQueueProducer.enqueueEvent(sampleMessage);

    // Then
    assertThat(result).isTrue();
    verify(streamOperations).add(any(StringRecord.class));
    verify(objectMapper, times(2)).writeValueAsString(any());
  }

  @Test
  @DisplayName("Should return false when stream add returns null")
  void enqueueEvent_ReturnsFalse_WhenStreamAddReturnsNull() throws Exception {
    // Given
    when(streamOperations.add(any(StringRecord.class))).thenReturn(null);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"key\":\"value\"}");

    // When
    boolean result = eventQueueProducer.enqueueEvent(sampleMessage);

    // Then
    assertThat(result).isFalse();
    verify(streamOperations).add(any(StringRecord.class));
  }

  @Test
  @DisplayName("Should return false when exception occurs during enqueue")
  void enqueueEvent_ReturnsFalse_WhenExceptionOccurs() throws Exception {
    // Given
    when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON error"));

    // When
    boolean result = eventQueueProducer.enqueueEvent(sampleMessage);

    // Then
    assertThat(result).isFalse();
    verify(objectMapper).writeValueAsString(any());
    verify(streamOperations, never()).add(any());
  }

  @Test
  @DisplayName("Should enqueue event with retry successfully")
  void enqueueEventWithRetry_Success() throws Exception {
    // Given
    RecordId expectedRecordId = RecordId.of("1234567890-0");
    when(streamOperations.add(any(StringRecord.class)))
        .thenReturn(null) // First attempt fails
        .thenReturn(expectedRecordId); // Second attempt succeeds
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"key\":\"value\"}");

    // When
    boolean result = eventQueueProducer.enqueueEventWithRetry(sampleMessage, 3);

    // Then
    assertThat(result).isTrue();
    verify(streamOperations, times(2)).add(any(StringRecord.class));
  }

  @Test
  @DisplayName("Should return false after max retries")
  void enqueueEventWithRetry_ReturnsFalse_AfterMaxRetries() throws Exception {
    // Given
    when(streamOperations.add(any(StringRecord.class))).thenReturn(null);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"key\":\"value\"}");

    // When
    boolean result = eventQueueProducer.enqueueEventWithRetry(sampleMessage, 3);

    // Then
    assertThat(result).isFalse();
    verify(streamOperations, times(3)).add(any(StringRecord.class));
  }

  @Test
  @DisplayName("Should handle idempotency check successfully")
  void enqueueEventWithIdempotencyCheck_Success() throws Exception {
    // Given
    RecordId expectedRecordId = RecordId.of("1234567890-0");
    when(redisTemplate.hasKey(anyString())).thenReturn(false);
    when(streamOperations.add(any(StringRecord.class))).thenReturn(expectedRecordId);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"key\":\"value\"}");

    // When
    boolean result = eventQueueProducer.enqueueEventWithIdempotencyCheck(sampleMessage);

    // Then
    assertThat(result).isTrue();
    verify(redisTemplate).hasKey(anyString());
    verify(valueOperations).set(anyString(), eq("processed"), eq(86400L));
    verify(streamOperations).add(any(StringRecord.class));
  }

  @Test
  @DisplayName("Should skip processing when idempotency key exists")
  void enqueueEventWithIdempotencyCheck_Skips_WhenKeyExists() {
    // Given
    when(redisTemplate.hasKey(anyString())).thenReturn(true);

    // When
    boolean result = eventQueueProducer.enqueueEventWithIdempotencyCheck(sampleMessage);

    // Then
    assertThat(result).isTrue(); // Returns true for idempotency
    verify(redisTemplate).hasKey(anyString());
    verify(redisTemplate, never()).opsForValue();
    verify(streamOperations, never()).add(any());
  }

  @Test
  @DisplayName("Should return false when idempotency check fails")
  void enqueueEventWithIdempotencyCheck_ReturnsFalse_WhenFails() throws Exception {
    // Given
    when(redisTemplate.hasKey(anyString())).thenReturn(false);
    when(streamOperations.add(any(StringRecord.class))).thenReturn(null);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"key\":\"value\"}");

    // When
    boolean result = eventQueueProducer.enqueueEventWithIdempotencyCheck(sampleMessage);

    // Then
    assertThat(result).isFalse();
    verify(redisTemplate).hasKey(anyString());
    verify(redisTemplate, never()).opsForValue(); // Should not set idempotency key on failure
    verify(streamOperations, times(3)).add(any(StringRecord.class));
  }

  @Test
  @DisplayName("Should return true when queue is healthy")
  void isQueueHealthy_ReturnsTrue_WhenHealthy() {
    // Given
    when(streamOperations.info(anyString()))
        .thenReturn(
            mock(org.springframework.data.redis.connection.stream.StreamInfo.XInfoStream.class));

    // When
    boolean result = eventQueueProducer.isQueueHealthy();

    // Then
    assertThat(result).isTrue();
    verify(streamOperations).info("events:stream");
  }

  @Test
  @DisplayName("Should return false when queue is unhealthy")
  void isQueueHealthy_ReturnsFalse_WhenUnhealthy() {
    // Given
    when(streamOperations.info(anyString())).thenThrow(new RuntimeException("Connection failed"));

    // When
    boolean result = eventQueueProducer.isQueueHealthy();

    // Then
    assertThat(result).isFalse();
    verify(streamOperations).info("events:stream");
  }

  @Test
  @DisplayName("Should handle null headers gracefully")
  void enqueueEvent_HandlesNullHeaders() throws Exception {
    // Given
    sampleMessage.setHeaders(null);
    RecordId expectedRecordId = RecordId.of("1234567890-0");
    when(streamOperations.add(any(StringRecord.class))).thenReturn(expectedRecordId);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"key\":\"value\"}");

    // When
    boolean result = eventQueueProducer.enqueueEvent(sampleMessage);

    // Then
    assertThat(result).isTrue();
    verify(streamOperations).add(any(StringRecord.class));
    verify(objectMapper, times(1)).writeValueAsString(any()); // Should serialize payload but not headers
    verify(objectMapper, never()).writeValueAsString(null); // Headers should not be serialized
  }

  @Test
  @DisplayName("Should handle null payload gracefully")
  void enqueueEvent_HandlesNullPayload() throws Exception {
    // Given
    sampleMessage.setPayload(null);
    RecordId expectedRecordId = RecordId.of("1234567890-0");
    when(streamOperations.add(any(StringRecord.class))).thenReturn(expectedRecordId);
    when(objectMapper.writeValueAsString(any())).thenReturn("null");

    // When
    boolean result = eventQueueProducer.enqueueEvent(sampleMessage);

    // Then
    assertThat(result).isTrue();
    verify(streamOperations).add(any(StringRecord.class));
    verify(objectMapper).writeValueAsString(null);
  }
}
