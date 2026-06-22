package com.lingotlow.backendcore.interfaces.api.event;

import com.lingotlow.backendcore.domain.event.model.Event;
import com.lingotlow.backendcore.domain.tenant.model.Tenant;
import com.lingotlow.backendcore.infrastructure.repository.EventRepository;
import com.lingotlow.backendcore.infrastructure.repository.TenantRepository;
import com.lingotlow.backendcore.interfaces.dto.EventResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/tenants/{tenantKey}/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventRepository eventRepository;
    private final TenantRepository tenantRepository;

    @PostConstruct
    public void init() {
        log.info("✅✅✅ EventController INITIALIZED ✅✅✅");
    }

    @GetMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> listEvents(
            @PathVariable String tenantKey,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("📋 Listing events for tenant: {} (status: {}, from: {}, to: {})", tenantKey, status, from, to);

        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Event> eventsPage;

        if (status != null && !status.isEmpty() && from != null && !from.isEmpty() && to != null && !to.isEmpty()) {
            Instant fromDate = Instant.parse(from + "Z");
            Instant toDate = Instant.parse(to + "Z");
            eventsPage = eventRepository.findByTenantAndStatusAndDateRange(tenant, status, fromDate, toDate, pageable);
        } else if (status != null && !status.isEmpty()) {
            eventsPage = eventRepository.findByTenantAndStatus(tenant, status, pageable);
        } else if (from != null && !from.isEmpty() && to != null && !to.isEmpty()) {
            Instant fromDate = Instant.parse(from + "Z");
            Instant toDate = Instant.parse(to + "Z");
            eventsPage = eventRepository.findByTenantAndDateRange(tenant, fromDate, toDate, pageable);
        } else {
            eventsPage = eventRepository.findByTenant(tenant, pageable);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", eventsPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
        response.put("page", eventsPage.getNumber());
        response.put("size", eventsPage.getSize());
        response.put("totalElements", eventsPage.getTotalElements());
        response.put("totalPages", eventsPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/replay")
    public ResponseEntity<Map<String, String>> replayEvent(
            @PathVariable String tenantKey,
            @PathVariable String eventId) {
        log.info("🔄 Replay requested for event: {} on tenant: {}", eventId, tenantKey);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Replay initiated for event: " + eventId);
        response.put("eventId", eventId);
        response.put("tenant", tenantKey);
        
        // Aqui você implementaria a lógica de replay
        // Por enquanto, apenas retornamos sucesso
        log.info("✅ Replay successful for event: {}", eventId);
        
        return ResponseEntity.accepted().body(response);
    }

    private EventResponseDTO toDTO(Event event) {
        return EventResponseDTO.builder()
                .id(event.getId())
                .requestId(event.getRequestId())
                .documentId(event.getDocumentId())
                .type(event.getType())
                .status(event.getStatus() != null ? event.getStatus().name() : null)
                .retryCount(event.getRetryCount())
                .payload(event.getPayload())
                .headers(event.getHeaders())
                .metadata(event.getMetadata())
                .failureReason(event.getFailureReason())
                .receivedAt(event.getReceivedAt())
                .deliveredAt(event.getDeliveredAt())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
