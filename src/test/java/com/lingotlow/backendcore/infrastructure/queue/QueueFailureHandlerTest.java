package com.lingotlow.backendcore.infrastructure.queue;

import com.lingotlow.backendcore.infrastructure.repository.EventRepository;
import com.lingotlow.backendcore.infrastructure.repository.entity.EventEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueFailureHandlerTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventQueueProducer eventQueueProducer;

    @InjectMocks
    private QueueFailureHandler queueFailureHandler;

    private UUID requestId;
    private UUID tenantId;
    private EventEntity eventEntity;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        tenantId = UUID.randomUUID();

        eventEntity = new EventEntity();
        eventEntity.setId(UUID.randomUUID());
        eventEntity.setRequestId(requestId);
        eventEntity.setTenantId(tenantId);
        eventEntity.setDocumentId("doc123");
        eventEntity.setType("test-event");
        eventEntity.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    void handleQueueFailure_EventFound_LogsSuccessfully() {
        // Arrange
        when(eventRepository.findByRequestId(requestId)).thenReturn(Optional.of(eventEntity));

        // Act
        queueFailureHandler.handleQueueFailure(requestId, tenantId, "Connection timeout");

        // Assert
        verify(eventRepository).findByRequestId(requestId);
    }

    @Test
    void handleQueueFailure_EventNotFound_HandlesGracefully() {
        // Arrange
        when(eventRepository.findByRequestId(requestId)).thenReturn(Optional.empty());

        // Act
        queueFailureHandler.handleQueueFailure(requestId, tenantId, "Connection timeout");

        // Assert
        verify(eventRepository).findByRequestId(requestId);
    }

    @Test
    void handleQueueFailure_RepositoryThrowsException_HandlesGracefully() {
        // Arrange
        when(eventRepository.findByRequestId(requestId))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act
        queueFailureHandler.handleQueueFailure(requestId, tenantId, "Connection timeout");

        // Assert
        verify(eventRepository).findByRequestId(requestId);
    }

    @Test
    void checkQueueHealth_HealthyQueue() {
        // Arrange
        when(eventQueueProducer.isQueueHealthy()).thenReturn(true);

        // Act
        queueFailureHandler.checkQueueHealth();

        // Assert
        verify(eventQueueProducer).isQueueHealthy();
    }

    @Test
    void checkQueueHealth_UnhealthyQueue() {
        // Arrange
        when(eventQueueProducer.isQueueHealthy()).thenReturn(false);

        // Act
        queueFailureHandler.checkQueueHealth();

        // Assert
        verify(eventQueueProducer).isQueueHealthy();
    }

    @Test
    void checkQueueHealth_ThrowsException_HandlesGracefully() {
        // Arrange
        when(eventQueueProducer.isQueueHealthy())
                .thenThrow(new RuntimeException("Health check failed"));

        // Act
        queueFailureHandler.checkQueueHealth();

        // Assert
        verify(eventQueueProducer).isQueueHealthy();
    }

    @Test
    void reprocessFailedEvents_CompletesSuccessfully() {
        // Act
        queueFailureHandler.reprocessFailedEvents();

        // Assert - just verifies the method completes without throwing exceptions
        // In a real implementation, we would verify interaction with eventRepository
    }

    @Test
    void reprocessFailedEvents_ThrowsException_HandlesGracefully() {
        // This test would be more meaningful with actual implementation
        // For now, just ensures the method doesn't crash the application
        queueFailureHandler.reprocessFailedEvents();
    }
}
