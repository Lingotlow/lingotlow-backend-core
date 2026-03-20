package com.lingotlow.backendcore.infrastructure.repository;

import com.lingotlow.backendcore.infrastructure.repository.entity.EventEntity;
import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("EventRepository Integration Tests")
class EventRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantId;
    private String documentId;
    private UUID requestId;

    @BeforeEach
    void setUp() {
        // Clean up
        eventRepository.deleteAll();
        tenantRepository.deleteAll();
        
        // Create test tenant
        TenantEntity tenant = new TenantEntity();
        tenant.setTenantKey("test-tenant");
        tenant.setName("Test Tenant");
        tenant.setConfig("{\"test\": true}");
        tenant.setCreatedAt(OffsetDateTime.now());
        tenant.setUpdatedAt(OffsetDateTime.now());
        
        tenant = entityManager.persistAndFlush(tenant);
        tenantId = tenant.getId();
        
        // Test data
        documentId = "doc-123";
        requestId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should find event by tenantId and documentId")
    void findByTenantIdAndDocumentId_Success() {
        // Given
        EventEntity event = createSampleEvent();
        entityManager.persistAndFlush(event);

        // When
        Optional<EventEntity> result = eventRepository.findByTenantIdAndDocumentId(tenantId, documentId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isEqualTo(tenantId);
        assertThat(result.get().getDocumentId()).isEqualTo(documentId);
        assertThat(result.get().getRequestId()).isEqualTo(requestId);
    }

    @Test
    @DisplayName("Should return empty when event not found by tenantId and documentId")
    void findByTenantIdAndDocumentId_ReturnsEmpty_WhenNotFound() {
        // When
        Optional<EventEntity> result = eventRepository.findByTenantIdAndDocumentId(tenantId, "non-existent-doc");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return true when event exists by tenantId and documentId")
    void existsByTenantIdAndDocumentId_ReturnsTrue_WhenExists() {
        // Given
        EventEntity event = createSampleEvent();
        entityManager.persistAndFlush(event);

        // When
        boolean result = eventRepository.existsByTenantIdAndDocumentId(tenantId, documentId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when event does not exist by tenantId and documentId")
    void existsByTenantIdAndDocumentId_ReturnsFalse_WhenDoesNotExist() {
        // When
        boolean result = eventRepository.existsByTenantIdAndDocumentId(tenantId, "non-existent-doc");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should find event by requestId")
    void findByRequestId_Success() {
        // Given
        EventEntity event = createSampleEvent();
        entityManager.persistAndFlush(event);

        // When
        Optional<EventEntity> result = eventRepository.findByRequestId(requestId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getRequestId()).isEqualTo(requestId);
        assertThat(result.get().getTenantId()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should return empty when event not found by requestId")
    void findByRequestId_ReturnsEmpty_WhenNotFound() {
        // When
        Optional<EventEntity> result = eventRepository.findByRequestId(UUID.randomUUID());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple events for same tenant with different documentIds")
    void handleMultipleEvents_SameTenant_DifferentDocumentIds() {
        // Given
        EventEntity event1 = createSampleEvent("doc-1", UUID.randomUUID());
        EventEntity event2 = createSampleEvent("doc-2", UUID.randomUUID());
        EventEntity event3 = createSampleEvent("doc-3", UUID.randomUUID());
        
        entityManager.persistAndFlush(event1);
        entityManager.persistAndFlush(event2);
        entityManager.persistAndFlush(event3);

        // When
        Optional<EventEntity> result1 = eventRepository.findByTenantIdAndDocumentId(tenantId, "doc-1");
        Optional<EventEntity> result2 = eventRepository.findByTenantIdAndDocumentId(tenantId, "doc-2");
        Optional<EventEntity> result3 = eventRepository.findByTenantIdAndDocumentId(tenantId, "doc-3");

        // Then
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        assertThat(result3).isPresent();
        
        assertThat(result1.get().getDocumentId()).isEqualTo("doc-1");
        assertThat(result2.get().getDocumentId()).isEqualTo("doc-2");
        assertThat(result3.get().getDocumentId()).isEqualTo("doc-3");
    }

    @Test
    @DisplayName("Should handle events from different tenants with same documentId")
    void handleEvents_DifferentTenants_SameDocumentId() {
        // Given
        UUID tenant2Id = UUID.randomUUID();
        TenantEntity tenant2 = new TenantEntity();
        tenant2.setTenantKey("test-tenant-2");
        tenant2.setName("Test Tenant 2");
        tenant2.setConfig("{\"test\": true}");
        tenant2.setCreatedAt(OffsetDateTime.now());
        tenant2.setUpdatedAt(OffsetDateTime.now());
        tenant2 = entityManager.persistAndFlush(tenant2);
        tenant2Id = tenant2.getId();

        EventEntity event1 = createSampleEventForTenant(tenantId, documentId, UUID.randomUUID());
        EventEntity event2 = createSampleEventForTenant(tenant2Id, documentId, UUID.randomUUID());
        
        entityManager.persistAndFlush(event1);
        entityManager.persistAndFlush(event2);

        // When
        Optional<EventEntity> result1 = eventRepository.findByTenantIdAndDocumentId(tenantId, documentId);
        Optional<EventEntity> result2 = eventRepository.findByTenantIdAndDocumentId(tenant2Id, documentId);

        // Then
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        
        assertThat(result1.get().getTenantId()).isEqualTo(tenantId);
        assertThat(result2.get().getTenantId()).isEqualTo(tenant2Id);
        
        // Both should have same documentId but different tenants
        assertThat(result1.get().getDocumentId()).isEqualTo(documentId);
        assertThat(result2.get().getDocumentId()).isEqualTo(documentId);
    }

    @Test
    @DisplayName("Should handle null documentId in exists check")
    void existsByTenantIdAndDocumentId_HandlesNullDocumentId() {
        // Given
        EventEntity eventWithNullDocId = createSampleEvent();
        eventWithNullDocId.setDocumentId(null);
        entityManager.persistAndFlush(eventWithNullDocId);

        // When
        boolean result = eventRepository.existsByTenantIdAndDocumentId(tenantId, null);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should handle empty documentId in exists check")
    void existsByTenantIdAndDocumentId_HandlesEmptyDocumentId() {
        // Given
        EventEntity eventWithEmptyDocId = createSampleEvent();
        eventWithEmptyDocId.setDocumentId("");
        entityManager.persistAndFlush(eventWithEmptyDocId);

        // When
        boolean result = eventRepository.existsByTenantIdAndDocumentId(tenantId, "");

        // Then
        assertThat(result).isTrue();
    }

    // Helper methods
    private EventEntity createSampleEvent() {
        return createSampleEvent(documentId, requestId);
    }

    private EventEntity createSampleEvent(String docId, UUID reqId) {
        return createSampleEventForTenant(tenantId, docId, reqId);
    }

    private EventEntity createSampleEventForTenant(UUID tId, String docId, UUID reqId) {
        EventEntity event = new EventEntity();
        event.setTenantId(tId);
        event.setRequestId(reqId);
        event.setDocumentId(docId);
        event.setType("test.event");
        event.setStatus(EventEntity.EventStatus.PENDING);
        event.setCreatedAt(OffsetDateTime.now());
        event.setUpdatedAt(OffsetDateTime.now());
        event.setSourceIp("192.168.1.100");
        event.setRetryCount(0);
        return event;
    }
}
