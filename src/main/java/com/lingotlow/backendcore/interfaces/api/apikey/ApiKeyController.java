package com.lingotlow.backendcore.interfaces.api.apikey;

import com.lingotlow.backendcore.domain.apikey.ApiKeyService;
import com.lingotlow.backendcore.domain.apikey.model.ApiKey;
import com.lingotlow.backendcore.interfaces.dto.ApiKeyRequest;
import com.lingotlow.backendcore.interfaces.dto.ApiKeyResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tenants/{tenantKey}/apikeys")
@RequiredArgsConstructor
@Slf4j
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    public ResponseEntity<ApiKeyResponse> generateApiKey(
            @PathVariable String tenantKey,
            @Valid @RequestBody ApiKeyRequest request) {
        log.info("Generating API Key for tenant: {}", tenantKey);
        String rawKey = apiKeyService.generateApiKey(tenantKey);
        return ResponseEntity.ok(new ApiKeyResponse(rawKey));
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> listApiKeys(@PathVariable String tenantKey) {
        log.info("Listing API Keys for tenant: {}", tenantKey);
        List<ApiKey> apiKeys = apiKeyService.getApiKeysByTenant(tenantKey);
        List<ApiKeyResponse> responses = apiKeys.stream()
                .map(key -> new ApiKeyResponse(key.getId(), key.getPrefix(), key.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> revokeApiKey(
            @PathVariable String tenantKey,
            @PathVariable UUID keyId) {
        log.info("Revoking API Key for tenant: {}", tenantKey);
        apiKeyService.revokeApiKey(keyId, tenantKey);
        return ResponseEntity.noContent().build();
    }
}