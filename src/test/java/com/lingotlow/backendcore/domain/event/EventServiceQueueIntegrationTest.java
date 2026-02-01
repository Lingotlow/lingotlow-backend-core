package com.lingotlow.backendcore.domain.event;

import com.lingotlow.backendcore.domain.event.mapper.EventServiceMapper;
import com.lingotlow.backendcore.domain.event.model.EventRequestDTO;
import com.lingotlow.backendcore.domain.event.model.EventResponseDTO;
import com.lingotlow.backendcore.infrastructure.logging.AuditLogger;
import com.lingotlow.backendcore.infrastructure.queue.EventQueueProducer;
import com.lingotlow.backendcore.infrastructure.repository.EventRepository;
import com.lingotlow.backendcore.infrastructure.repository.TenantRepository;
import com.lingotlow.backendcore.infrastructure.repository.entity.EventEntity;
import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceQueueIntegrationTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private EventServiceMapper eventMapper;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private EventQueueProducer eventQueueProducer;

    @InjectMocks
    private EventService eventService;

    private TenantEntity tenant;
    private EventRequestDTO eventRequest;
    private EventEntity savedEvent;

    @BeforeEach
    void setUp() {
        // Setup tenant
        tenant = new TenantEntity();
        tenant.setId(UUID.randomUUID());
        tenant.setTenantKey("test-tenant");
        tenant.setName("Test Tenant");
        tenant.setCreatedAt(OffsetDateTime.now());

        // Setup event request
        eventRequest = new EventRequestDTO();
        eventRequest.setDocumentId("doc123");
        eventRequest.setType("test-event");
        eventRequest.setTimestamp(System.currentTimeMillis());
        eventRequest.setPayload("{}");

        // Setup saved event
        savedEvent = new EventEntity();
        savedEvent.setId(UUID.randomUUID());
        savedEvent.setRequestId(UUID.randomUUID());
        savedEvent.setTenantId(tenant.getId());
        savedEvent.setDocumentId("doc123");
        savedEvent.setType("test-event");
        savedEvent.setCreatedAt(OffsetDateTime.now());
        savedEvent.setSourceIp("127.0.0.1");
    }

    @Test
    void createEvent_SuccessfulQueueEnqueue() {
        // Arrange
        when(tenantRepository.findByTenantKey(anyString())).thenReturn(Optional.of(tenant));
        when(eventRepository.existsByTenantIdAndDocumentId(any(), anyString())).thenReturn(false);
        when(eventMapper.mapToCreateEntity(any(), any(), any())).thenReturn(savedEvent);
        when(eventRepository.save(any())).thenReturn(savedEvent);
        when(eventMapper.mapToResponseDTO(any())).thenReturn(new EventResponseDTO());
        when(eventQueueProducer.enqueueEventWithIdempotencyCheck(any())).thenReturn(true);

        // Act
        var result = eventService.createEvent("test-tenant", eventRequest, "127.0.0.1");

        // Assert
        assertNotNull(result);
        verify(eventRepository).save(any());
        verify(eventQueueProducer).enqueueEventWithIdempotencyCheck(any());
        verify(auditLogger).logEventCreated(anyString(), anyString(), anyString(), any());
    }

    @Test
    void createEvent_QueueFails_EventStillCreated() {
        // Arrange
        when(tenantRepository.findByTenantKey(anyString())).thenReturn(Optional.of(tenant));
        when(eventRepository.existsByTenantIdAndDocumentId(any(), anyString())).thenReturn(false);
        when(eventMapper.mapToCreateEntity(any(), any(), any())).thenReturn(savedEvent);
        when(eventRepository.save(any())).thenReturn(savedEvent);
        when(eventMapper.mapToResponseDTO(any())).thenReturn(new EventResponseDTO());
        when(eventQueueProducer.enqueueEventWithIdempotencyCheck(any())).thenReturn(false);

        // Act
        var result = eventService.createEvent("test-tenant", eventRequest, "127.0.0.1");

        // Assert
        assertNotNull(result);
        verify(eventRepository).save(any());
        verify(eventQueueProducer).enqueueEventWithIdempotencyCheck(any());
        verify(auditLogger).logEventCreated(anyString(), anyString(), anyString(), any());

        // Verify that audit log contains enqueued: false
        var auditDataCaptor = new HashMap<String, Object>();
        verify(auditLogger).logEventCreated(
                eq("test-tenant"),
                anyString(),
                eq("127.0.0.1"),
                argThat(data -> {
                    Map<String, Object> auditData = (Map<String, Object>) data;
                    return auditData.containsKey("enqueued") && auditData.get("enqueued").equals(false);
                })
        );
    }

    @Test
    void createEvent_QueueThrowsException_EventStillCreated() {
        // Arrange
        when(tenantRepository.findByTenantKey(anyString())).thenReturn(Optional.of(tenant));
        when(eventRepository.existsByTenantIdAndDocumentId(any(), anyString())).thenReturn(false);
        when(eventMapper.mapToCreateEntity(any(), any(), any())).thenReturn(savedEvent);
        when(eventRepository.save(any())).thenReturn(savedEvent);
        when(eventMapper.mapToResponseDTO(any())).thenReturn(new EventResponseDTO());
        when(eventQueueProducer.enqueueEventWithIdempotencyCheck(any())).thenThrow(new RuntimeException("Redis connection failed"));

        // Act
        var result = eventService.createEvent("test-tenant", eventRequest, "127.0.0.1");

        // Assert
        assertNotNull(result);
        verify(eventRepository).save(any());
        verify(eventQueueProducer).enqueueEventWithIdempotencyCheck(any());
        verify(auditLogger).logEventCreated(anyString(), anyString(), anyString(), any());
    }

    @Test
    void createEvent_QueueMessageContent() {
        // Arrange
        when(tenantRepository.findByTenantKey(anyString())).thenReturn(Optional.of(tenant));
        when(eventRepository.existsByTenantIdAndDocumentId(any(), anyString())).thenReturn(false);
        when(eventMapper.mapToCreateEntity(any(), any(), any())).thenReturn(savedEvent);
        when(eventRepository.save(any())).thenReturn(savedEvent);
        when(eventMapper.mapToResponseDTO(any())).thenReturn(new EventResponseDTO());
        when(eventQueueProducer.enqueueEventWithIdempotencyCheck(any())).thenReturn(true);

        // Act
        eventService.createEvent("test-tenant", eventRequest, "127.0.0.1");

        // Assert
        verify(eventQueueProducer).enqueueEventWithIdempotencyCheck(argThat(message -> {
            EventQueueProducer.EventQueueMessage queueMessage = message;
            return queueMessage.getRequestId().equals(savedEvent.getRequestId()) &&
                    queueMessage.getTenantId().equals(tenant.getId()) &&
                    queueMessage.getDocumentId().equals("doc123") &&
                    queueMessage.getType().equals("test-event") &&
                    queueMessage.getStatus().equals("PENDING") &&
                    queueMessage.getPayload() != null &&
                    queueMessage.getHeaders() != null &&
                    queueMessage.getHeaders().containsKey("tenantKey") &&
                    queueMessage.getHeaders().get("tenantKey").equals("test-tenant");
        }));
    }

    @Test
    void createEvent_TenantNotFound_QueueNotCalled() {
        // Arrange
        when(tenantRepository.findByTenantKey(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(com.lingotlow.backendcore.interfaces.api.exception.ResourceNotFoundException.class, () -> {
            eventService.createEvent("invalid-tenant", eventRequest, "127.0.0.1");
        });

        verify(eventQueueProducer, never()).enqueueEventWithIdempotencyCheck(any());
    }

    @Test
    void createEvent_DuplicateDocument_QueueNotCalled() {
        // Arrange
        when(tenantRepository.findByTenantKey(anyString())).thenReturn(Optional.of(tenant));
        when(eventRepository.existsByTenantIdAndDocumentId(any(), anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(com.lingotlow.backendcore.interfaces.api.exception.ResourceAlreadyExistsException.class, () -> {
            eventService.createEvent("test-tenant", eventRequest, "127.0.0.1");
        });

        verify(eventQueueProducer, never()).enqueueEventWithIdempotencyCheck(any());
    }
}
