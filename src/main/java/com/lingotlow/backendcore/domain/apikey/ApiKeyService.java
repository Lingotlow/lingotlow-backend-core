package com.lingotlow.backendcore.domain.apikey;

import com.lingotlow.backendcore.domain.apikey.cryto.ApiKeyCryptoService;
import com.lingotlow.backendcore.domain.apikey.model.ApiKeyResponseDTO;
import com.lingotlow.backendcore.infrastructure.repository.ApiKeyRepository;
import com.lingotlow.backendcore.infrastructure.repository.entity.ApiKeyEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyCryptoService cryptoService;

    public ApiKeyResponseDTO createApiKey(UUID tenantId) {
        String plainKey = cryptoService.generateApiKey();
        String hash = cryptoService.hashApiKey(plainKey);
        //TODO deve usar mapper toEntity
//        ApiKeyEntity apiKey = new ApiKeyEntity(UUID.randomUUID(), tenantId, hash, OffsetDateTime.now(), false);
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKeyRepository.save(apiKey);
        return new ApiKeyResponseDTO(apiKey, plainKey);
    }

    public List<ApiKeyEntity> listApiKeys(UUID tenantId) {
        // TODO
        // return apiKeyRepository.findByTenantId(tenantId);
        return null;
    }

    public void revokeApiKey(UUID tenantId, UUID keyId) {
        ApiKeyEntity apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new RuntimeException("API Key não encontrada"));

        if (!apiKey.getTenantId().equals(tenantId)) {
            throw new RuntimeException("API Key não pertence ao tenant");
        }

//        apiKey.revoke(); //TODO
        apiKeyRepository.save(apiKey);
    }

    public boolean validateApiKey(UUID tenantId, String plainKey) {
        //TODO
//        Optional<ApiKeyEntity> apiKeyOpt = apiKeyRepository.findByTenantId(tenantId).stream()
//                .filter(k -> !k.isRevoked() && cryptoService.matches(plainKey, k.getKeyHash()))
//                .findFirst();
//        return apiKeyOpt.isPresent();
        return false;
    }
}
