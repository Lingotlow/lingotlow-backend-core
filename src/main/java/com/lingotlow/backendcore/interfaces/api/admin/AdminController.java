package com.lingotlow.backendcore.interfaces.api.admin;

import com.lingotlow.backendcore.infrastructure.queue.EventQueueProducer;
import com.lingotlow.backendcore.infrastructure.repository.EventRepository;
import com.lingotlow.backendcore.infrastructure.repository.entity.EventEntity;
import com.lingotlow.backendcore.infrastructure.retry.RetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Operations", description = "Administrative operations for system management")
public class AdminController {

    private final EventRepository eventRepository;
    private final EventQueueProducer eventQueueProducer;
    private final RetryService retryService;

    @PostMapping("/events/{requestId}/replay")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Replay a failed event",
            description = "Re-enqueues a failed event for processing")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200", 
                            description = "Event replayed successfully",
                            content = @Content(schema = @Schema(implementation = ReplayResponse.class))),
                    @ApiResponse(
                            responseCode = "404", 
                            description = "Event not found"),
                    @ApiResponse(
                            responseCode = "400", 
                            description = "Event cannot be replayed")
            })
    public ResponseEntity<ReplayResponse> replayEvent(
            @Parameter(description = "Event request ID") @PathVariable UUID requestId) {
        
        log.info("Admin requested replay for event: {}", requestId);

        EventEntity event = eventRepository.findByRequestId(requestId)
                .orElseThrow(() -> {
                    log.warn("Event not found for replay: {}", requestId);
                    return new RuntimeException("Event not found: " + requestId);
                });

        // Check if event can be replayed
        if (event.getStatus() == EventEntity.EventStatus.PENDING || 
            event.getStatus() == EventEntity.EventStatus.DELIVERED) {
            return ResponseEntity.badRequest()
                    .body(new ReplayResponse(false, "Event cannot be replayed: " + event.getStatus()));
        }

        try {
            // Reset event status
            event.setStatus(EventEntity.EventStatus.PENDING);
            event.setFailureReason(null);
            event.setRetryCount(0);
            eventRepository.save(event);

            // Re-enqueue for processing
            EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                    event.getRequestId(),
                    event.getTenantId(),
                    event.getDocumentId(),
                    event.getType(),
                    "REPLAY",
                    Map.of("replayedBy", "admin", "replayedAt", System.currentTimeMillis())
            );

            boolean enqueued = eventQueueProducer.enqueueEventWithIdempotencyCheck(message);

            if (enqueued) {
                log.info("Event successfully replayed: {}", requestId);
                return ResponseEntity.ok(new ReplayResponse(true, "Event replayed successfully"));
            } else {
                log.warn("Failed to re-enqueue replayed event: {}", requestId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ReplayResponse(false, "Failed to re-enqueue event"));
            }

        } catch (Exception e) {
            log.error("Error replaying event: {}", requestId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ReplayResponse(false, "Error replaying event: " + e.getMessage()));
        }
    }

    @GetMapping("/events/{requestId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get event status",
            description = "Returns the current status and details of an event")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200", 
                            description = "Event status retrieved"),
                    @ApiResponse(
                            responseCode = "404", 
                            description = "Event not found")
            })
    public ResponseEntity<EventStatusResponse> getEventStatus(
            @Parameter(description = "Event request ID") @PathVariable UUID requestId) {
        
        log.debug("Admin requested status for event: {}", requestId);

        EventEntity event = eventRepository.findByRequestId(requestId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + requestId));

        EventStatusResponse response = new EventStatusResponse(
                event.getRequestId(),
                event.getTenantId(),
                event.getDocumentId(),
                event.getType(),
                event.getStatus(),
                event.getFailureReason(),
                event.getRetryCount(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );

        return ResponseEntity.ok(response);
    }

    // DTOs
    public static class ReplayResponse {
        private final boolean success;
        private final String message;

        public ReplayResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    public static class EventStatusResponse {
        private final UUID requestId;
        private final UUID tenantId;
        private final String documentId;
        private final String type;
        private final EventEntity.EventStatus status;
        private final String failureReason;
        private final Integer retryCount;
        private final java.time.OffsetDateTime createdAt;
        private final java.time.OffsetDateTime updatedAt;

        public EventStatusResponse(UUID requestId, UUID tenantId, String documentId, String type, 
                                EventEntity.EventStatus status, String failureReason, Integer retryCount,
                                java.time.OffsetDateTime createdAt, java.time.OffsetDateTime updatedAt) {
            this.requestId = requestId;
            this.tenantId = tenantId;
            this.documentId = documentId;
            this.type = type;
            this.status = status;
            this.failureReason = failureReason;
            this.retryCount = retryCount;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        // Getters
        public UUID getRequestId() { return requestId; }
        public UUID getTenantId() { return tenantId; }
        public String getDocumentId() { return documentId; }
        public String getType() { return type; }
        public EventEntity.EventStatus getStatus() { return status; }
        public String getFailureReason() { return failureReason; }
        public Integer getRetryCount() { return retryCount; }
        public java.time.OffsetDateTime getCreatedAt() { return createdAt; }
        public java.time.OffsetDateTime getUpdatedAt() { return updatedAt; }
    }
}
