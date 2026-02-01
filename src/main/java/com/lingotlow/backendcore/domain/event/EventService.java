package com.lingotlow.backendcore.domain.event;

import com.lingotlow.backendcore.domain.event.mapper.EventServiceMapper;
import com.lingotlow.backendcore.domain.event.model.EventRequestDTO;
import com.lingotlow.backendcore.domain.event.model.EventResponseDTO;
import com.lingotlow.backendcore.infrastructure.logging.AuditLogger;
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

    public EventService(
            EventRepository eventRepository,
            TenantRepository tenantRepository,
            EventServiceMapper eventMapper,
            AuditLogger auditLogger) {
        this.eventRepository = eventRepository;
        this.tenantRepository = tenantRepository;
        this.eventMapper = eventMapper;
        this.auditLogger = auditLogger;
    }

    public EventResponseDTO createEvent(String tenantKey, EventRequestDTO request, String sourceIp) {
        log.info("Creating event for tenant: {} with documentId: {}", tenantKey, request.getDocumentId());

        // Validar tenant
        TenantEntity tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> {
                    log.warn("Event creation failed - tenant not found: {}", tenantKey);
                    return new ResourceNotFoundException("Tenant", tenantKey);
                });

        // Verificar idempotência (evitar duplicação)
        if (request.getDocumentId() != null && eventRepository.existsByTenantIdAndDocumentId(tenant.getId(), request.getDocumentId())) {
            log.warn("Event creation failed - documentId already exists for tenant: {} documentId: {}", tenantKey, request.getDocumentId());
            throw new ResourceAlreadyExistsException("Event", request.getDocumentId());
        }

        // Gerar requestId único
        UUID requestId = UUID.randomUUID();

        // Criar entidade do evento
        EventEntity eventEntity = eventMapper.mapToCreateEntity(request, tenant.getId(), requestId);
        eventEntity.setSourceIp(sourceIp);

        // Salvar evento
        EventEntity savedEvent = eventRepository.save(eventEntity);

        log.info("Event created successfully - tenantId: {}, requestId: {}, documentId: {}", 
                tenant.getId(), requestId, request.getDocumentId());

        // Log de auditoria
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("documentId", request.getDocumentId());
        auditData.put("type", request.getType());
        auditData.put("timestamp", request.getTimestamp());
        
        auditLogger.logEventCreated(
                tenantKey,
                requestId.toString(),
                sourceIp,
                auditData
        );

        // Retornar resposta
        EventResponseDTO response = eventMapper.mapToResponseDTO(savedEvent);
        response.setMessage("Event received successfully");

        return response;
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
