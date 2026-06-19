package com.lingotlow.backendcore.interfaces.api.endpoint;

import com.lingotlow.backendcore.domain.endpoint.model.Endpoint;
import com.lingotlow.backendcore.infrastructure.service.EndpointService;
import com.lingotlow.backendcore.interfaces.dto.EndpointRequest;
import com.lingotlow.backendcore.interfaces.dto.EndpointResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/tenants/{tenantKey}/endpoints")
@RequiredArgsConstructor
@Slf4j
public class EndpointController {

    private final EndpointService endpointService;

    @PostMapping
    public ResponseEntity<EndpointResponse> createEndpoint(
            @PathVariable String tenantKey,
            @Valid @RequestBody EndpointRequest request) {

        log.info("Creating endpoint for tenant: {}", tenantKey);

        Endpoint endpoint = Endpoint.builder()
                .name(request.getName())
                .url(request.getUrl())
                .retryCount(request.getRetryCount())
                .timeoutMs(request.getTimeoutMs())
                .secret(request.getSecret())
                .active(request.getActive())
                .description(request.getDescription())
                .build();

        Endpoint created = endpointService.createEndpoint(tenantKey, endpoint);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<EndpointResponse>> listEndpoints(@PathVariable String tenantKey) {
        log.info("Listing endpoints for tenant: {}", tenantKey);
        List<Endpoint> endpoints = endpointService.getEndpointsByTenant(tenantKey);
        List<EndpointResponse> responses = endpoints.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{endpointId}")
    public ResponseEntity<EndpointResponse> getEndpoint(
            @PathVariable String tenantKey,
            @PathVariable UUID endpointId) {
        log.info("Getting endpoint for tenant: {}, id: {}", tenantKey, endpointId);
        Endpoint endpoint = endpointService.getEndpointById(tenantKey, endpointId);
        return ResponseEntity.ok(toResponse(endpoint));
    }

    @PutMapping("/{endpointId}")
    public ResponseEntity<EndpointResponse> updateEndpoint(
            @PathVariable String tenantKey,
            @PathVariable UUID endpointId,
            @Valid @RequestBody EndpointRequest request) {

        log.info("Updating endpoint for tenant: {}, id: {}", tenantKey, endpointId);

        Endpoint endpoint = Endpoint.builder()
                .name(request.getName())
                .url(request.getUrl())
                .retryCount(request.getRetryCount())
                .timeoutMs(request.getTimeoutMs())
                .secret(request.getSecret())
                .active(request.getActive())
                .description(request.getDescription())
                .build();

        Endpoint updated = endpointService.updateEndpoint(tenantKey, endpointId, endpoint);
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{endpointId}")
    public ResponseEntity<Void> deleteEndpoint(
            @PathVariable String tenantKey,
            @PathVariable UUID endpointId) {
        log.info("Deleting endpoint for tenant: {}, id: {}", tenantKey, endpointId);
        endpointService.deleteEndpoint(tenantKey, endpointId);
        return ResponseEntity.noContent().build();
    }

    private EndpointResponse toResponse(Endpoint endpoint) {
        return EndpointResponse.builder()
                .id(endpoint.getId())
                .name(endpoint.getName())
                .url(endpoint.getUrl())
                .retryCount(endpoint.getRetryCount())
                .timeoutMs(endpoint.getTimeoutMs())
                .active(endpoint.getActive())
                .status(endpoint.getStatus())
                .description(endpoint.getDescription())
                .createdAt(endpoint.getCreatedAt())
                .updatedAt(endpoint.getUpdatedAt())
                .build();
    }
}