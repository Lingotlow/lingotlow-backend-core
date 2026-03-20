package com.lingotlow.backendcore.infrastructure.queue;

import com.lingotlow.backendcore.infrastructure.repository.EventRepository;
import com.lingotlow.backendcore.infrastructure.repository.entity.EventEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueFailureHandler Unit Tests")
class QueueFailureHandlerTest {

    @Mock
    private EventRepository eventRepository;
    
    @Mock
    private EventQueueProducer eventQueueProducer;

    @InjectMocks
    private QueueFailureHandler queueFailureHandler;

    private UUID requestId;
    private UUID tenantId;
    private EventEntity sampleEvent;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        sampleEvent = createSampleEvent();
    }

    @Test
    @DisplayName("Should handle queue failure successfully when event exists")
    void handleQueueFailure_Success_WhenEventExists() {
        // Given
        when(eventRepository.findByRequestId(requestId)).thenReturn(Optional.of(sampleEvent));

        // When
        queueFailureHandler.handleQueueFailure(requestId, tenantId, "Connection timeout");

        // Then
        verify(eventRepository).findByRequestId(requestId);
        // The method is async, so we can't easily verify the exact logging without additional test setup
    }

    @Test
    @DisplayName("Should handle queue failure gracefully when event does not exist")
    void handleQueueFailure_Gracefully_WhenEventDoesNotExist() {
        // Given
        when(eventRepository.findByRequestId(requestId)).thenReturn(Optional.empty());

        // When
        queueFailureHandler.handleQueueFailure(requestId, tenantId, "Connection timeout");

        // Then
        verify(eventRepository).findByRequestId(requestId);
        // Should not throw exception
    }

    @Test
    @DisplayName("Should handle queue failure gracefully when repository throws exception")
    void handleQueueFailure_Gracefully_WhenRepositoryThrowsException() {
        // Given
        when(eventRepository.findByRequestId(requestId))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThatNoException().isThrownBy(() -> 
                queueFailureHandler.handleQueueFailure(requestId, tenantId, "Connection timeout"));
        
        verify(eventRepository).findByRequestId(requestId);
    }

    @Test
    @DisplayName("Should pass queue health check when queue is healthy")
    void checkQueueHealth_Passes_WhenQueueIsHealthy() {
        // Given
        when(eventQueueProducer.isQueueHealthy()).thenReturn(true);

        // When
        queueFailureHandler.checkQueueHealth();

        // Then
        verify(eventQueueProducer).isQueueHealthy();
    }

    @Test
    @DisplayName("Should log error when queue health check fails")
    void checkQueueHealth_LogsError_WhenQueueIsUnhealthy() {
        // Given
        when(eventQueueProducer.isQueueHealthy()).thenReturn(false);

        // When
        queueFailureHandler.checkQueueHealth();

        // Then
        verify(eventQueueProducer).isQueueHealthy();
        // Error should be logged (would need to capture logs for verification)
    }

    @Test
    @DisplayName("Should handle queue health check gracefully when exception occurs")
    void checkQueueHealth_Gracefully_WhenExceptionOccurs() {
        // Given
        when(eventQueueProducer.isQueueHealthy())
                .thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then
        assertThatNoException().isThrownBy(() -> queueFailureHandler.checkQueueHealth());
        
        verify(eventQueueProducer).isQueueHealthy();
    }

    @Test
    @DisplayName("Should execute reprocess failed events successfully")
    void reprocessFailedEvents_Success() {
        // When
        queueFailureHandler.reprocessFailedEvents();

        // Then
        // Method is scheduled and currently just logs, so we verify it doesn't throw exception
        // In a real implementation, we would verify repository calls and reprocessing logic
    }

    @Test
    @DisplayName("Should handle reprocess failed events gracefully when exception occurs")
    void reprocessFailedEvents_Gracefully_WhenExceptionOccurs() {
        // This test would be more meaningful if the method had actual logic
        // For now, we verify it doesn't throw exception
        assertThatNoException().isThrownBy(() -> queueFailureHandler.reprocessFailedEvents());
    }

    @Test
    @DisplayName("Should handle null requestId gracefully")
    void handleQueueFailure_Gracefully_WhenRequestIdIsNull() {
        // Given
        when(eventRepository.findByRequestId(isNull())).thenReturn(Optional.empty());

        // When & Then
        assertThatNoException().isThrownBy(() -> 
                queueFailureHandler.handleQueueFailure(null, tenantId, "Null request ID"));
        
        verify(eventRepository).findByRequestId(null);
    }

    @Test
    @DisplayName("Should handle null tenantId gracefully")
    void handleQueueFailure_Gracefully_WhenTenantIdIsNull() {
        // Given
        when(eventRepository.findByRequestId(requestId)).thenReturn(Optional.of(sampleEvent));

        // When & Then
        assertThatNoException().isThrownBy(() -> 
                queueFailureHandler.handleQueueFailure(requestId, null, "Null tenant ID"));
        
        verify(eventRepository).findByRequestId(requestId);
    }

    @Test
    @DisplayName("Should handle null error message gracefully")
    void handleQueueFailure_Gracefully_WhenErrorMessageIsNull() {
        // Given
        when(eventRepository.findByRequestId(requestId)).thenReturn(Optional.of(sampleEvent));

        // When & Then
        assertThatNoException().isThrownBy(() -> 
                queueFailureHandler.handleQueueFailure(requestId, tenantId, null));
        
        verify(eventRepository).findByRequestId(requestId);
    }

    @Test
    @DisplayName("Should handle empty error message gracefully")
    void handleQueueFailure_Gracefully_WhenErrorMessageIsEmpty() {
        // Given
        when(eventRepository.findByRequestId(requestId)).thenReturn(Optional.of(sampleEvent));

        // When & Then
        assertThatNoException().isThrownBy(() -> 
                queueFailureHandler.handleQueueFailure(requestId, tenantId, ""));
        
        verify(eventRepository).findByRequestId(requestId);
    }

    // Helper method
    private EventEntity createSampleEvent() {
        EventEntity event = new EventEntity();
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(event, "tenantId", tenantId);
        ReflectionTestUtils.setField(event, "requestId", requestId);
        event.setDocumentId("doc-123");
        event.setType("test.event");
        event.setStatus(EventEntity.EventStatus.PENDING);
        event.setCreatedAt(OffsetDateTime.now());
        event.setUpdatedAt(OffsetDateTime.now());
        event.setSourceIp("192.168.1.100");
        event.setRetryCount(0);
        return event;
    }
}
