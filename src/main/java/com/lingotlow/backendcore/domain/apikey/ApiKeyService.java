package com.lingotlow.backendcore.domain.apikey;

import com.lingotlow.backendcore.domain.apikey.cryto.ApiKeyCryptoService;
import com.lingotlow.backendcore.domain.apikey.mapper.ApiKeyServiceMapper;
import com.lingotlow.backendcore.domain.apikey.model.ApiKeyResponseDTO;
import com.lingotlow.backendcore.infrastructure.repository.ApiKeyRepository;
import com.lingotlow.backendcore.infrastructure.repository.entity.ApiKeyEntity;
import com.lingotlow.backendcore.interfaces.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyCryptoService cryptoService;
    private final ApiKeyServiceMapper apiKeyServiceMapper;

    public ApiKeyResponseDTO createApiKey(UUID tenantId) {
        log.info("Creating API key for tenant: {}", tenantId);

        String plainKey = cryptoService.generateApiKey();
        String hash = cryptoService.hashApiKey(plainKey);
        ApiKeyEntity apiKey = apiKeyServiceMapper.mapToEntity(UUID.randomUUID(), tenantId, hash, OffsetDateTime.now(), false);
        apiKeyRepository.save(apiKey);

        log.info("API key created successfully with ID: {} for tenant: {}", apiKey.getId(), tenantId);
        return apiKeyServiceMapper.mapToResponse(apiKey, plainKey);
    }

    public List<ApiKeyEntity> listApiKeys(UUID tenantId) {
        log.info("Listing API keys for tenant: {}", tenantId);
        List<ApiKeyEntity> apiKeys = apiKeyRepository.findAllActiveByTenant(tenantId);
        log.info("Found {} active API keys for tenant: {}", apiKeys.size(), tenantId);
        return apiKeys;
    }

    public void revokeApiKey(UUID tenantId, UUID keyId) {
        log.info("Revoking API key: {} for tenant: {}", keyId, tenantId);

        ApiKeyEntity apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API Key", keyId.toString()));

        if (!apiKey.getTenantId().equals(tenantId)) {
            log.warn("API key: {} does not belong to tenant: {}", keyId, tenantId);
            throw new IllegalArgumentException("API Key não pertence ao tenant");
        }

        apiKey.setRevoked(true);
        apiKeyRepository.save(apiKey);

        log.info("API key: {} revoked successfully for tenant: {}", keyId, tenantId);
    }

    public boolean validateApiKey(UUID tenantId, String plainKey) {
        log.debug("Validating API key for tenant: {}", tenantId);

        if (tenantId == null || plainKey == null || plainKey.trim().isEmpty()) {
            log.debug("API key validation failed: tenantId or plainKey is null/empty");
            return false;
        }

        boolean isValid = apiKeyRepository.findByTenantId(tenantId).stream()
                .anyMatch(k -> !k.isRevoked() && cryptoService.matches(plainKey, k.getKeyHash()));

        log.debug("API key validation for tenant: {} result: {}", tenantId, isValid);
        return isValid;
    }
}
