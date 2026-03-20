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
import com.lingotlow.backendcore.interfaces.api.exception.ResourceAlreadyExistsException;
import com.lingotlow.backendcore.interfaces.api.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final TenantRepository tenantRepository;
    private final EventServiceMapper eventMapper;
    private final AuditLogger auditLogger;
    private final EventQueueProducer eventQueueProducer;

    public EventService(
            EventRepository eventRepository,
            TenantRepository tenantRepository,
            EventServiceMapper eventMapper,
            AuditLogger auditLogger,
            EventQueueProducer eventQueueProducer) {
        this.eventRepository = eventRepository;
        this.tenantRepository = tenantRepository;
        this.eventMapper = eventMapper;
        this.auditLogger = auditLogger;
        this.eventQueueProducer = eventQueueProducer;
    }

    public EventResponseDTO createEvent(String tenantKey, EventRequestDTO request, String sourceIp) {
        log.info("Creating event for tenant: {} with documentId: {}", tenantKey, request.getDocumentId());

        // Validate tenant
        TenantEntity tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> {
                    log.warn("Event creation failed - tenant not found: {}", tenantKey);
                    return new ResourceNotFoundException("Tenant", tenantKey);
                });

        // Check idempotency (avoid duplication)
        if (request.getDocumentId() != null && eventRepository.existsByTenantIdAndDocumentId(tenant.getId(), request.getDocumentId())) {
            log.warn("Event creation failed - documentId already exists for tenant: {} documentId: {}", tenantKey, request.getDocumentId());
            throw new ResourceAlreadyExistsException("Event", request.getDocumentId());
        }

        // Generate unique requestId
        UUID requestId = UUID.randomUUID();

        // Create event entity
        EventEntity eventEntity = eventMapper.mapToCreateEntity(request, tenant.getId(), requestId);
        eventEntity.setSourceIp(sourceIp);
        eventEntity.setStatus(EventEntity.EventStatus.PENDING);
        eventEntity.setRetryCount(0);

        // Save event
        EventEntity savedEvent = eventRepository.save(eventEntity);

        log.info("Event created successfully - tenantId: {}, requestId: {}, documentId: {}",
                tenant.getId(), requestId, request.getDocumentId());

        // Enqueue event for async processing
        boolean enqueued = enqueueEventForProcessing(savedEvent, tenant);

        if (!enqueued) {
            log.error("Failed to enqueue event for async processing - tenantId: {}, requestId: {}",
                    tenant.getId(), requestId);
            // Event creation is not failed, but error is logged
        }

        // Audit log
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("documentId", request.getDocumentId());
        auditData.put("type", request.getType());
        auditData.put("timestamp", request.getTimestamp());
        auditData.put("enqueued", enqueued);

        auditLogger.logEventCreated(
                tenantKey,
                requestId.toString(),
                sourceIp,
                auditData
        );

        // Return response
        EventResponseDTO response = eventMapper.mapToResponseDTO(savedEvent);
        response.setMessage("Event received successfully");

        return response;
    }

    private boolean enqueueEventForProcessing(EventEntity event, TenantEntity tenant) {
        try {
            log.debug("Enqueuing event for async processing - tenantId: {}, requestId: {}",
                    tenant.getId(), event.getRequestId());

            // Prepare payload for queue
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", event.getType());
            payload.put("documentId", event.getDocumentId());
            payload.put("timestamp", event.getCreatedAt());
            payload.put("sourceIp", event.getSourceIp());

            // Prepare headers
            Map<String, String> headers = new HashMap<>();
            headers.put("tenantKey", tenant.getTenantKey());
            headers.put("userAgent", "backend-core");

            // Create message for queue
            EventQueueProducer.EventQueueMessage queueMessage = new EventQueueProducer.EventQueueMessage(
                    event.getRequestId(),
                    tenant.getId(),
                    event.getDocumentId(),
                    event.getType(),
                    "PENDING",
                    payload
            );
            queueMessage.setHeaders(headers);

            // Try to enqueue with retry and idempotency
            boolean enqueued = eventQueueProducer.enqueueEventWithIdempotencyCheck(queueMessage);

            if (enqueued) {
                log.info("Event successfully enqueued for async processing - tenantId: {}, requestId: {}",
                        tenant.getId(), event.getRequestId());
            } else {
                log.warn("Event enqueue failed after retries - tenantId: {}, requestId: {}",
                        tenant.getId(), event.getRequestId());
            }

            return enqueued;

        } catch (Exception e) {
            log.error("Error preparing event for queue - tenantId: {}, requestId: {}, error: {}",
                    tenant.getId(), event.getRequestId(), e.getMessage(), e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public EventEntity getEventByRequestId(UUID requestId) {
        log.debug("Retrieving event with requestId: {}", requestId);

        return eventRepository.findByRequestId(requestId)
                .orElseThrow(() -> {
                    log.warn("Event not found: {}", requestId);
                    return new ResourceNotFoundException("Event", requestId.toString());
                });
    }

    @Transactional(readOnly = true)
    public boolean eventExistsForTenantAndDocument(UUID tenantId, String documentId) {
        boolean exists = eventRepository.existsByTenantIdAndDocumentId(tenantId, documentId);
        log.debug("Event existence check for tenantId: {} documentId: {}: {}", tenantId, documentId, exists);
        return exists;
    }
}
