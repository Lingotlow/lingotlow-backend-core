package com.lingotlow.backendcore.domain.event;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lingotlow.backendcore.domain.event.mapper.EventServiceMapper;
import com.lingotlow.backendcore.domain.event.model.EventRequestDTO;
import com.lingotlow.backendcore.domain.event.model.EventResponseDTO;
import com.lingotlow.backendcore.infrastructure.logging.AuditLogger;
import com.lingotlow.backendcore.infrastructure.queue.EventQueueProducer;
import com.lingotlow.backendcore.infrastructure.repository.EventRepository;
import com.lingotlow.backendcore.infrastructure.repository.TenantRepository;
import com.lingotlow.backendcore.infrastructure.repository.entity.EventEntity;
import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import com.lingotlow.backendcore.interfaces.api.exception.ResourceAlreadyExistsException;
import com.lingotlow.backendcore.interfaces.api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Unit Tests")
class EventServiceTest {

    private final String TENANT_KEY = "test-tenant";
    private final UUID TENANT_ID = UUID.randomUUID();
    private final String DOCUMENT_ID = "doc-123";
    private final String EVENT_TYPE = "invoice.created";
    private final Long TIMESTAMP = System.currentTimeMillis();
    private final String SOURCE_IP = "192.168.1.100";

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

    private TenantEntity sampleTenant;
    private EventRequestDTO sampleRequestDTO;
    private EventEntity sampleEventEntity;
    private EventResponseDTO sampleResponseDTO;

    @BeforeEach
    void setUp() {
        sampleTenant = createSampleTenant();
        sampleRequestDTO = createSampleRequestDTO();
        sampleEventEntity = createSampleEventEntity();
        sampleResponseDTO = createSampleResponseDTO();
    }

    @Test
    @DisplayName("Should create event successfully when tenant exists and documentId is unique")
    void createEvent_Success_WhenTenantExistsAndDocumentIdIsUnique() {
        // Given
        when(tenantRepository.findByTenantKey(TENANT_KEY)).thenReturn(Optional.of(sampleTenant));
        when(eventRepository.existsByTenantIdAndDocumentId(TENANT_ID, DOCUMENT_ID)).thenReturn(false);
        when(eventMapper.mapToCreateEntity(any(EventRequestDTO.class), eq(TENANT_ID), any(UUID.class)))
                .thenReturn(sampleEventEntity);
        when(eventRepository.save(sampleEventEntity)).thenReturn(sampleEventEntity);
        when(eventMapper.mapToResponseDTO(sampleEventEntity)).thenReturn(sampleResponseDTO);

        // When
        EventResponseDTO result = eventService.createEvent(TENANT_KEY, sampleRequestDTO, SOURCE_IP);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRequestId()).isEqualTo(sampleEventEntity.getRequestId());
        assertThat(result.getStatus()).isEqualTo("RECEIVED");
        assertThat(result.getMessage()).isEqualTo("Event received successfully");

        verify(tenantRepository).findByTenantKey(TENANT_KEY);
        verify(eventRepository).existsByTenantIdAndDocumentId(TENANT_ID, DOCUMENT_ID);
        verify(eventMapper).mapToCreateEntity(any(EventRequestDTO.class), eq(TENANT_ID), any(UUID.class));
        verify(eventRepository).save(sampleEventEntity);
        verify(auditLogger).logEventCreated(eq(TENANT_KEY), anyString(), eq(SOURCE_IP), any(Map.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when tenant does not exist")
    void createEvent_ThrowsException_WhenTenantDoesNotExist() {
        // Given
        when(tenantRepository.findByTenantKey(TENANT_KEY)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.createEvent(TENANT_KEY, sampleRequestDTO, SOURCE_IP))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tenant")
                .hasMessageContaining(TENANT_KEY);

        verify(tenantRepository).findByTenantKey(TENANT_KEY);
        verify(eventRepository, never()).existsByTenantIdAndDocumentId(any(), any());
        verify(eventRepository, never()).save(any());
        verify(auditLogger, never()).logEventCreated(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when documentId already exists for tenant")
    void createEvent_ThrowsException_WhenDocumentIdAlreadyExists() {
        // Given
        when(tenantRepository.findByTenantKey(TENANT_KEY)).thenReturn(Optional.of(sampleTenant));
        when(eventRepository.existsByTenantIdAndDocumentId(TENANT_ID, DOCUMENT_ID)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> eventService.createEvent(TENANT_KEY, sampleRequestDTO, SOURCE_IP))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Event")
                .hasMessageContaining(DOCUMENT_ID);

        verify(tenantRepository).findByTenantKey(TENANT_KEY);
        verify(eventRepository).existsByTenantIdAndDocumentId(TENANT_ID, DOCUMENT_ID);
        verify(eventRepository, never()).save(any());
        verify(auditLogger, never()).logEventCreated(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should create event successfully when documentId is null (no idempotency check)")
    void createEvent_Success_WhenDocumentIdIsNull() {
        // Given
        EventRequestDTO requestWithNullDocumentId = createSampleRequestDTO();
        requestWithNullDocumentId.setDocumentId(null);
        
        when(tenantRepository.findByTenantKey(TENANT_KEY)).thenReturn(Optional.of(sampleTenant));
        when(eventMapper.mapToCreateEntity(any(EventRequestDTO.class), eq(TENANT_ID), any(UUID.class)))
                .thenReturn(sampleEventEntity);
        when(eventRepository.save(sampleEventEntity)).thenReturn(sampleEventEntity);
        when(eventMapper.mapToResponseDTO(sampleEventEntity)).thenReturn(sampleResponseDTO);

        // When
        EventResponseDTO result = eventService.createEvent(TENANT_KEY, requestWithNullDocumentId, SOURCE_IP);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRequestId()).isEqualTo(sampleEventEntity.getRequestId());

        verify(tenantRepository).findByTenantKey(TENANT_KEY);
        verify(eventRepository, never()).existsByTenantIdAndDocumentId(any(), any());
        verify(eventRepository).save(sampleEventEntity);
        verify(auditLogger).logEventCreated(eq(TENANT_KEY), anyString(), eq(SOURCE_IP), any(Map.class));
    }

    @Test
    @DisplayName("Should return event when requestId exists")
    void getEventByRequestId_Success_WhenEventExists() {
        // Given
        UUID requestId = sampleEventEntity.getRequestId();
        when(eventRepository.findByRequestId(requestId)).thenReturn(Optional.of(sampleEventEntity));

        // When
        EventEntity result = eventService.getEventByRequestId(requestId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRequestId()).isEqualTo(requestId);
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);

        verify(eventRepository).findByRequestId(requestId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when requestId does not exist")
    void getEventByRequestId_ThrowsException_WhenEventDoesNotExist() {
        // Given
        UUID requestId = UUID.randomUUID();
        when(eventRepository.findByRequestId(requestId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.getEventByRequestId(requestId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event")
                .hasMessageContaining(requestId.toString());

        verify(eventRepository).findByRequestId(requestId);
    }

    @Test
    @DisplayName("Should return true when event exists for tenant and documentId")
    void eventExistsForTenantAndDocument_ReturnsTrue_WhenEventExists() {
        // Given
        when(eventRepository.existsByTenantIdAndDocumentId(TENANT_ID, DOCUMENT_ID)).thenReturn(true);

        // When
        boolean result = eventService.eventExistsForTenantAndDocument(TENANT_ID, DOCUMENT_ID);

        // Then
        assertThat(result).isTrue();
        verify(eventRepository).existsByTenantIdAndDocumentId(TENANT_ID, DOCUMENT_ID);
    }

    @Test
    @DisplayName("Should return false when event does not exist for tenant and documentId")
    void eventExistsForTenantAndDocument_ReturnsFalse_WhenEventDoesNotExist() {
        // Given
        when(eventRepository.existsByTenantIdAndDocumentId(TENANT_ID, DOCUMENT_ID)).thenReturn(false);

        // When
        boolean result = eventService.eventExistsForTenantAndDocument(TENANT_ID, DOCUMENT_ID);

        // Then
        assertThat(result).isFalse();
        verify(eventRepository).existsByTenantIdAndDocumentId(TENANT_ID, DOCUMENT_ID);
    }

    @Test
    @DisplayName("Should create event successfully even when queue fails")
    void createEvent_Success_EvenWhenQueueFails() {
        // Given
        when(tenantRepository.findByTenantKey(TENANT_KEY)).thenReturn(Optional.of(sampleTenant));
        when(eventRepository.existsByTenantIdAndDocumentId(TENANT_ID, DOCUMENT_ID)).thenReturn(false);
        when(eventMapper.mapToCreateEntity(any(EventRequestDTO.class), eq(TENANT_ID), any(UUID.class)))
                .thenReturn(sampleEventEntity);
        when(eventRepository.save(sampleEventEntity)).thenReturn(sampleEventEntity);
        when(eventMapper.mapToResponseDTO(sampleEventEntity)).thenReturn(sampleResponseDTO);
        
        // Mock queue failure
        when(eventQueueProducer.enqueueEventWithIdempotencyCheck(any())).thenReturn(false);

        // When
        EventResponseDTO result = eventService.createEvent(TENANT_KEY, sampleRequestDTO, SOURCE_IP);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRequestId()).isEqualTo(sampleEventEntity.getRequestId());
        assertThat(result.getMessage()).isEqualTo("Event received successfully");

        verify(tenantRepository).findByTenantKey(TENANT_KEY);
        verify(eventRepository).save(sampleEventEntity);
        verify(eventQueueProducer).enqueueEventWithIdempotencyCheck(any());
        verify(auditLogger).logEventCreated(eq(TENANT_KEY), anyString(), eq(SOURCE_IP), any(Map.class));
    }

    @Test
    @DisplayName("Should handle invalid payload gracefully")
    void createEvent_HandlesInvalidPayload() {
        // Given
        EventRequestDTO requestWithInvalidPayload = createSampleRequestDTO();
        requestWithInvalidPayload.setPayload(null); // Invalid payload
        
        when(tenantRepository.findByTenantKey(TENANT_KEY)).thenReturn(Optional.of(sampleTenant));
        when(eventRepository.existsByTenantIdAndDocumentId(TENANT_ID, DOCUMENT_ID)).thenReturn(false);
        when(eventMapper.mapToCreateEntity(any(EventRequestDTO.class), eq(TENANT_ID), any(UUID.class)))
                .thenReturn(sampleEventEntity);
        when(eventRepository.save(sampleEventEntity)).thenReturn(sampleEventEntity);
        when(eventMapper.mapToResponseDTO(sampleEventEntity)).thenReturn(sampleResponseDTO);
        when(eventQueueProducer.enqueueEventWithIdempotencyCheck(any())).thenReturn(true);

        // When
        EventResponseDTO result = eventService.createEvent(TENANT_KEY, requestWithInvalidPayload, SOURCE_IP);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRequestId()).isEqualTo(sampleEventEntity.getRequestId());
        
        verify(tenantRepository).findByTenantKey(TENANT_KEY);
        verify(eventRepository).save(sampleEventEntity);
        verify(eventQueueProducer).enqueueEventWithIdempotencyCheck(any());
    }

    @Test
    @DisplayName("Should handle empty documentId for idempotency")
    void createEvent_HandlesEmptyDocumentId() {
        // Given
        EventRequestDTO requestWithEmptyDocumentId = createSampleRequestDTO();
        requestWithEmptyDocumentId.setDocumentId(""); // Empty but not null
        
        when(tenantRepository.findByTenantKey(TENANT_KEY)).thenReturn(Optional.of(sampleTenant));
        when(eventRepository.existsByTenantIdAndDocumentId(TENANT_ID, "")).thenReturn(false);
        when(eventMapper.mapToCreateEntity(any(EventRequestDTO.class), eq(TENANT_ID), any(UUID.class)))
                .thenReturn(sampleEventEntity);
        when(eventRepository.save(sampleEventEntity)).thenReturn(sampleEventEntity);
        when(eventMapper.mapToResponseDTO(sampleEventEntity)).thenReturn(sampleResponseDTO);
        when(eventQueueProducer.enqueueEventWithIdempotencyCheck(any())).thenReturn(true);

        // When
        EventResponseDTO result = eventService.createEvent(TENANT_KEY, requestWithEmptyDocumentId, SOURCE_IP);

        // Then
        assertThat(result).isNotNull();
        verify(eventRepository).existsByTenantIdAndDocumentId(TENANT_ID, "");
        verify(eventRepository).save(sampleEventEntity);
    }

    @Test
    @DisplayName("Should handle null source IP")
    void createEvent_HandlesNullSourceIp() {
        // Given
        when(tenantRepository.findByTenantKey(TENANT_KEY)).thenReturn(Optional.of(sampleTenant));
        when(eventRepository.existsByTenantIdAndDocumentId(TENANT_ID, DOCUMENT_ID)).thenReturn(false);
        when(eventMapper.mapToCreateEntity(any(EventRequestDTO.class), eq(TENANT_ID), any(UUID.class)))
                .thenReturn(sampleEventEntity);
        when(eventRepository.save(sampleEventEntity)).thenReturn(sampleEventEntity);
        when(eventMapper.mapToResponseDTO(sampleEventEntity)).thenReturn(sampleResponseDTO);
        when(eventQueueProducer.enqueueEventWithIdempotencyCheck(any())).thenReturn(true);

        // When
        EventResponseDTO result = eventService.createEvent(TENANT_KEY, sampleRequestDTO, null);

        // Then
        assertThat(result).isNotNull();
        verify(eventRepository).save(sampleEventEntity);
        verify(auditLogger).logEventCreated(eq(TENANT_KEY), anyString(), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("Should handle queue exception gracefully")
    void createEvent_HandlesQueueException() {
        // Given
        when(tenantRepository.findByTenantKey(TENANT_KEY)).thenReturn(Optional.of(sampleTenant));
        when(eventRepository.existsByTenantIdAndDocumentId(TENANT_ID, DOCUMENT_ID)).thenReturn(false);
        when(eventMapper.mapToCreateEntity(any(EventRequestDTO.class), eq(TENANT_ID), any(UUID.class)))
                .thenReturn(sampleEventEntity);
        when(eventRepository.save(sampleEventEntity)).thenReturn(sampleEventEntity);
        when(eventMapper.mapToResponseDTO(sampleEventEntity)).thenReturn(sampleResponseDTO);
        
        // Mock queue exception
        when(eventQueueProducer.enqueueEventWithIdempotencyCheck(any()))
                .thenThrow(new RuntimeException("Queue connection failed"));

        // When
        EventResponseDTO result = eventService.createEvent(TENANT_KEY, sampleRequestDTO, SOURCE_IP);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRequestId()).isEqualTo(sampleEventEntity.getRequestId());
        
        verify(eventQueueProducer).enqueueEventWithIdempotencyCheck(any());
        verify(auditLogger).logEventCreated(eq(TENANT_KEY), anyString(), eq(SOURCE_IP), any(Map.class));
    }

    // Helper methods
    private TenantEntity createSampleTenant() {
        TenantEntity tenant = new TenantEntity();
        ReflectionTestUtils.setField(tenant, "id", TENANT_ID);
        tenant.setTenantKey(TENANT_KEY);
        tenant.setName("Test Tenant");
        tenant.setCreatedAt(OffsetDateTime.now());
        tenant.setUpdatedAt(OffsetDateTime.now());
        return tenant;
    }

    private EventRequestDTO createSampleRequestDTO() {
        EventRequestDTO dto = new EventRequestDTO();
        dto.setDocumentId(DOCUMENT_ID);
        dto.setType(EVENT_TYPE);
        dto.setTimestamp(TIMESTAMP);
        dto.setMetadata(Map.of("key1", "value1"));
        dto.setHeaders(Map.of("Content-Type", "application/json"));
        dto.setPayload("sample payload");
        return dto;
    }

    private EventEntity createSampleEventEntity() {
        EventEntity event = new EventEntity();
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(event, "tenantId", TENANT_ID);
        ReflectionTestUtils.setField(event, "requestId", UUID.randomUUID());
        event.setDocumentId(DOCUMENT_ID);
        event.setType(EVENT_TYPE);
        event.setStatus(EventEntity.EventStatus.PENDING);
        event.setCreatedAt(OffsetDateTime.now());
        event.setUpdatedAt(OffsetDateTime.now());
        event.setSourceIp(SOURCE_IP);
        event.setRetryCount(0);
        return event;
    }

    private EventResponseDTO createSampleResponseDTO() {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setRequestId(sampleEventEntity.getRequestId());
        dto.setStatus("PENDING");
        dto.setTimestamp(sampleEventEntity.getCreatedAt());
        dto.setMessage("Event received successfully");
        return dto;
    }
}
