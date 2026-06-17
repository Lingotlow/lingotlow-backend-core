package com.lingotlow.backendcore.domain.apikey;

import com.lingotlow.backendcore.domain.apikey.model.ApiKey;
import com.lingotlow.backendcore.domain.tenant.model.Tenant;
import com.lingotlow.backendcore.infrastructure.repository.ApiKeyRepository;
import com.lingotlow.backendcore.infrastructure.repository.TenantRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private static final String API_KEY_PREFIX = "lingotlow_";
    private final ApiKeyRepository apiKeyRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String generateApiKey(String tenantKey) {
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

        // Generate a secure API key
        String rawApiKey = API_KEY_PREFIX + UUID.randomUUID().toString().replace("-", "");
        String hashedKey = passwordEncoder.encode(rawApiKey);

        // Extract prefix for display
        String prefix = rawApiKey.substring(0, Math.min(15, rawApiKey.length()));

        ApiKey apiKey = ApiKey.builder()
                .tenant(tenant)
                .keyHash(hashedKey)
                .prefix(prefix)
                .build();

        apiKeyRepository.save(apiKey);
        log.info("API Key generated for tenant: {}", tenantKey);

        return rawApiKey;
    }

    @Transactional(readOnly = true)
    public boolean validateApiKey(String tenantKey, String rawApiKey) {
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

        List<ApiKey> apiKeys = apiKeyRepository.findByTenantAndRevokedFalse(tenant);

        return apiKeys.stream()
                .anyMatch(apiKey -> passwordEncoder.matches(rawApiKey, apiKey.getKeyHash()));
    }

    @Transactional(readOnly = true)
    public List<ApiKey> getApiKeysByTenant(String tenantKey) {
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

        return apiKeyRepository.findByTenantAndRevokedFalse(tenant);
    }

    @Transactional
    public void revokeApiKey(UUID keyId, String tenantKey) {
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API Key not found: " + keyId));

        if (!apiKey.getTenant().getId().equals(tenant.getId())) {
            throw new IllegalArgumentException("API Key does not belong to this tenant");
        }

        apiKey.setRevoked(true);
        apiKey.setRevokedAt(Instant.now());
        apiKeyRepository.save(apiKey);
        log.info("API Key revoked for tenant: {}", tenantKey);
    }
}