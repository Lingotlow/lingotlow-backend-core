package com.lingotlow.backendcore.infrastructure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingotlow.backendcore.domain.apikey.ApiKeyService;
import com.lingotlow.backendcore.domain.enums.EventStatus;
import com.lingotlow.backendcore.domain.event.model.Event;
import com.lingotlow.backendcore.domain.tenant.model.Tenant;
import com.lingotlow.backendcore.infrastructure.repository.EventRepository;
import com.lingotlow.backendcore.infrastructure.repository.TenantRepository;
import com.lingotlow.backendcore.interfaces.dto.IngestRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final TenantRepository tenantRepository;
    private final ApiKeyService apiKeyService;
    private final EventRepository eventRepository;
    private final RedisStreamService redisStreamService;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID ingestWebhook(String tenantKey, String apiKey, IngestRequest request, String idempotencyKey) {
        // 1. Validar tenant
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

        // 2. Validar API Key
        if (!apiKeyService.validateApiKey(tenantKey, apiKey)) {
            throw new SecurityException("Invalid API Key for tenant: " + tenantKey);
        }

        // 3. Verificar idempotência (se houver chave)
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            UUID requestId = UUID.nameUUIDFromBytes(idempotencyKey.getBytes());
            if (eventRepository.findByRequestId(requestId).isPresent()) {
                log.info("Idempotent request detected for key: {}, returning existing requestId: {}", idempotencyKey, requestId);
                return requestId;
            }
        }

        // 4. Criar e salvar evento
        UUID requestId = UUID.randomUUID();
        Event event = Event.builder()
                .tenant(tenant)
                .requestId(requestId)
                .documentId(request.getDocAttributes().getDocumentId())
                .type(request.getDocAttributes().getType())
                .status(EventStatus.RECEIVED)
                .payload(request.getPayload() != null ? toJson(request.getPayload()) : null)
                .headers(toJson(Map.of("source", request.getDocAttributes().getSource() != null ? request.getDocAttributes().getSource() : "unknown")))
                .metadata(request.getDocAttributes().getMetadata() != null ? toJson(request.getDocAttributes().getMetadata()) : null)
                .retryCount(0)
                .build();

        // Set receivedAt manually since builder might not have it
        event.setReceivedAt(Instant.now());
        event = eventRepository.save(event);
        log.info("Event saved with requestId: {}", requestId);

        // 5. Enfileirar para processamento
        Map<String, Object> queueMessage = new HashMap<>();
        queueMessage.put("eventId", event.getId().toString());
        queueMessage.put("requestId", requestId.toString());
        queueMessage.put("tenantKey", tenantKey);
        queueMessage.put("endpointId", null);
        queueMessage.put("receivedAt", event.getReceivedAt().toString());

        redisStreamService.publish("stream:events", queueMessage);
        log.info("Event enqueued for processing: {}", requestId);

        return requestId;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error converting to JSON", e);
            return "{}";
        }
    }
}