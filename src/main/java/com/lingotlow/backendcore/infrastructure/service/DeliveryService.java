package com.lingotlow.backendcore.infrastructure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingotlow.backendcore.domain.endpoint.model.Endpoint;
import com.lingotlow.backendcore.domain.event.model.Event;
import com.lingotlow.backendcore.domain.tenant.model.Tenant;
import com.lingotlow.backendcore.infrastructure.repository.EndpointRepository;
import com.lingotlow.backendcore.infrastructure.repository.EventRepository;
import com.lingotlow.backendcore.infrastructure.repository.TenantRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

  private final EventRepository eventRepository;
  private final EndpointRepository endpointRepository;
  private final TenantRepository tenantRepository;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Transactional
  public boolean deliverEvent(UUID eventId) {
    Event event =
        eventRepository
            .findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

    Tenant tenant = event.getTenant();

    // Buscar endpoint ativo do tenant
    List<Endpoint> endpoints = endpointRepository.findByTenantAndActiveTrue(tenant);
    if (endpoints.isEmpty()) {
      log.warn("No active endpoint found for tenant: {}", tenant.getTenantKey());
      event.setStatus(com.lingotlow.backendcore.domain.enums.EventStatus.FAILED);
      event.setFailureReason("No active endpoint configured");
      eventRepository.save(event);
      return false;
    }

    Endpoint endpoint = endpoints.get(0);

    // Preparar headers
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // Adicionar secret se configurado
    if (endpoint.getSecret() != null && !endpoint.getSecret().isEmpty()) {
      headers.set("X-Webhook-Secret", endpoint.getSecret());
    }

    // Preparar payload
    Map<String, Object> payload = new HashMap<>();
    payload.put("eventId", event.getId());
    payload.put("requestId", event.getRequestId());
    payload.put("documentId", event.getDocumentId());
    payload.put("type", event.getType());
    payload.put("timestamp", event.getReceivedAt().toString());
    payload.put("data", parsePayload(event.getPayload()));

    HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

    try {
      long startTime = System.currentTimeMillis();
      ResponseEntity<String> response =
          restTemplate.exchange(endpoint.getUrl(), HttpMethod.POST, requestEntity, String.class);
      long duration = System.currentTimeMillis() - startTime;

      if (response.getStatusCode().is2xxSuccessful()) {
        log.info(
            "Webhook delivered successfully to {} for event: {}", endpoint.getUrl(), event.getId());
        event.setStatus(com.lingotlow.backendcore.domain.enums.EventStatus.DELIVERED);
        event.setDeliveredAt(Instant.now());
        eventRepository.save(event);
        return true;
      } else {
        log.warn(
            "Webhook delivery failed with status {} for event: {}",
            response.getStatusCode(),
            event.getId());
        event.setFailureReason("HTTP " + response.getStatusCode() + ": " + response.getBody());
        eventRepository.save(event);
        return false;
      }
    } catch (Exception e) {
      log.error("Error delivering webhook for event {}: {}", event.getId(), e.getMessage());
      event.setFailureReason(e.getMessage());
      eventRepository.save(event);
      return false;
    }
  }

  private Map<String, Object> parsePayload(String payloadJson) {
    try {
      if (payloadJson != null && !payloadJson.isEmpty()) {
        return objectMapper.readValue(payloadJson, Map.class);
      }
    } catch (Exception e) {
      log.warn("Failed to parse payload: {}", e.getMessage());
    }
    return Map.of();
  }
}
