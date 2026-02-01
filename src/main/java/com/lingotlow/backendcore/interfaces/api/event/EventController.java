package com.lingotlow.backendcore.interfaces.api.event;

import com.lingotlow.backendcore.domain.event.EventService;
import com.lingotlow.backendcore.domain.event.model.EventResponseDTO;
import com.lingotlow.backendcore.interfaces.api.event.mapper.EventControllerMapper;
import com.lingotlow.backendcore.interfaces.api.event.model.EventIngestRequest;
import com.lingotlow.backendcore.interfaces.api.event.model.EventIngestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ingest/{tenantKey}")
@Tag(name = "Event Ingestion", description = "API for ingesting events")
public class EventController {

    private final EventService eventService;
    private final EventControllerMapper controllerMapper;

    public EventController(EventService eventService, EventControllerMapper controllerMapper) {
        this.eventService = eventService;
        this.controllerMapper = controllerMapper;
    }

    @PostMapping
    @Operation(
            summary = "Ingest event",
            description = "Receives and processes an event for a specific tenant")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200", 
                            description = "Event received successfully",
                            content = @Content(schema = @Schema(implementation = EventIngestResponse.class))),
                    @ApiResponse(
                            responseCode = "400", 
                            description = "Invalid request data",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(
                            responseCode = "404", 
                            description = "Tenant not found",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(
                            responseCode = "409", 
                            description = "Event already exists (duplicate documentId)",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(
                            responseCode = "500", 
                            description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = String.class)))
            })
    public ResponseEntity<EventIngestResponse> ingestEvent(
            @Parameter(description = "Tenant key") @PathVariable String tenantKey,
            @RequestBody @Valid EventIngestRequest request,
            HttpServletRequest httpRequest) {

        String sourceIp = getClientIpAddress(httpRequest);
        log.info("Received event ingestion request for tenant: {} from IP: {}", tenantKey, sourceIp);

        try {
            EventResponseDTO eventResponse = eventService.createEvent(
                    tenantKey, 
                    controllerMapper.mapToDomain(request), 
                    sourceIp
            );

            EventIngestResponse response = controllerMapper.mapToResponse(eventResponse);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing event ingestion for tenant: {}", tenantKey, e);
            throw e;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
