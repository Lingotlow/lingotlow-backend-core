package com.lingotlow.backendcore.interfaces.api.apikey;

import com.lingotlow.backendcore.domain.apikey.ApiKeyService;
import com.lingotlow.backendcore.domain.apikey.model.ApiKeyResponseDTO;
import com.lingotlow.backendcore.domain.tenant.TenantService;
import com.lingotlow.backendcore.interfaces.api.apikey.mapper.ApiKeyControllerMapper;
import com.lingotlow.backendcore.interfaces.api.apikey.model.ApiKeyResponse;
import com.lingotlow.backendcore.interfaces.api.apikey.model.CreateApiKeyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tenants/{tenantKey}/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final ApiKeyControllerMapper mapper;
    private final TenantService tenantService;

    private UUID resolveTenantId(String tenantKey) {
        return tenantService.getTenantByTenantKey(tenantKey).getId();
    }

    @Operation(summary = "Create or rotate API Key for tenant", description = "Creates a new API key for the specified tenant. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "API Key created",
                    content = @Content(schema = @Schema(implementation = CreateApiKeyResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CreateApiKeyResponse> createApiKey(
            @Parameter(in = ParameterIn.PATH, description = "Tenant key", required = true)
            @PathVariable String tenantKey) {
        UUID tenantId = resolveTenantId(tenantKey);

        ApiKeyResponseDTO result = apiKeyService.createApiKey(tenantId);

        CreateApiKeyResponse response = mapper.toCreateApiKeyResponseDTO(result.getApiKey(), result.getPlainKey());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List API Keys", description = "Lists API keys of the tenant without exposing secrets. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of API Keys",
                    content = @Content(schema = @Schema(implementation = ApiKeyResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> listApiKeys(
            @Parameter(in = ParameterIn.PATH, description = "Tenant key", required = true)
            @PathVariable String tenantKey) {
        UUID tenantId = resolveTenantId(tenantKey);

        List<ApiKeyResponse> response = apiKeyService.listApiKeys(tenantId)
                .stream()
                .map(mapper::toApiKeyResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Revoke API Key", description = "Revokes the API key of the tenant. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "API Key revoked"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Tenant or API Key not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> revokeApiKey(
            @Parameter(in = ParameterIn.PATH, description = "Tenant key", required = true)
            @PathVariable String tenantKey,
            @Parameter(in = ParameterIn.PATH, description = "API Key ID", required = true)
            @PathVariable UUID keyId) {
        UUID tenantId = resolveTenantId(tenantKey);
        apiKeyService.revokeApiKey(tenantId, keyId);
        return ResponseEntity.noContent().build();
    }
}
