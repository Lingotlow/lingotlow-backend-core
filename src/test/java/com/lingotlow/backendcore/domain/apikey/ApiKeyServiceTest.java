package com.lingotlow.backendcore.domain.apikey;

import com.lingotlow.backendcore.domain.apikey.cryto.ApiKeyCryptoService;
import com.lingotlow.backendcore.domain.apikey.mapper.ApiKeyServiceMapper;
import com.lingotlow.backendcore.domain.apikey.model.ApiKeyResponseDTO;
import com.lingotlow.backendcore.infrastructure.repository.ApiKeyRepository;
import com.lingotlow.backendcore.infrastructure.repository.entity.ApiKeyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApiKeyCryptoService cryptoService;

    @Mock
    private ApiKeyServiceMapper apiKeyServiceMapper;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private UUID tenantId;
    private UUID keyId;
    private String plainKey;
    private String hashedKey;
    private ApiKeyEntity apiKeyEntity;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        keyId = UUID.randomUUID();
        plainKey = "lt_test123456789";
        hashedKey = "hashed_key";

        apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setId(keyId);
        apiKeyEntity.setTenantId(tenantId);
        apiKeyEntity.setKeyHash(hashedKey);
        apiKeyEntity.setCreatedAt(OffsetDateTime.now());
        apiKeyEntity.setRevoked(false);
    }

    @Test
    void testCreateApiKey_Success() {
        // Given
        when(cryptoService.generateApiKey()).thenReturn(plainKey);
        when(cryptoService.hashApiKey(plainKey)).thenReturn(hashedKey);
        when(apiKeyServiceMapper.mapToEntity(any(UUID.class), eq(tenantId), eq(hashedKey), any(OffsetDateTime.class), eq(false)))
                .thenReturn(apiKeyEntity);
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenReturn(apiKeyEntity);

        ApiKeyResponseDTO expectedResponse = new ApiKeyResponseDTO(apiKeyEntity, plainKey);
        when(apiKeyServiceMapper.mapToResponse(apiKeyEntity, plainKey)).thenReturn(expectedResponse);

        // When
        ApiKeyResponseDTO result = apiKeyService.createApiKey(tenantId);

        // Then
        assertNotNull(result);
        assertEquals(apiKeyEntity, result.getApiKey());
        assertEquals(plainKey, result.getPlainKey());
        verify(cryptoService).generateApiKey();
        verify(cryptoService).hashApiKey(plainKey);
        verify(apiKeyRepository).save(any(ApiKeyEntity.class));
    }

    @Test
    void testListApiKeys_Success() {
        // Given
        List<ApiKeyEntity> expectedKeys = List.of(apiKeyEntity);
        when(apiKeyRepository.findAllActiveByTenant(tenantId)).thenReturn(expectedKeys);

        // When
        List<ApiKeyEntity> result = apiKeyService.listApiKeys(tenantId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(apiKeyEntity, result.getFirst());
        verify(apiKeyRepository).findAllActiveByTenant(tenantId);
    }

    @Test
    void testRevokeApiKey_Success() {
        // Given
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKeyEntity));

        // When
        apiKeyService.revokeApiKey(tenantId, keyId);

        // Then
        assertTrue(apiKeyEntity.isRevoked());
        verify(apiKeyRepository).save(apiKeyEntity);
    }

    @Test
    void testRevokeApiKey_NotFound() {
        // Given
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> apiKeyService.revokeApiKey(tenantId, keyId));
    }

    @Test
    void testRevokeApiKey_WrongTenant() {
        // Given
        UUID wrongTenantId = UUID.randomUUID();
        apiKeyEntity.setTenantId(wrongTenantId);
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKeyEntity));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> apiKeyService.revokeApiKey(tenantId, keyId));
    }

    @Test
    void testValidateApiKey_Success() {
        // Given
        when(apiKeyRepository.findByTenantId(tenantId)).thenReturn(List.of(apiKeyEntity));
        when(cryptoService.matches(plainKey, hashedKey)).thenReturn(true);

        // When
        boolean result = apiKeyService.validateApiKey(tenantId, plainKey);

        // Then
        assertTrue(result);
        verify(cryptoService).matches(plainKey, hashedKey);
    }

    @Test
    void testValidateApiKey_NotFound() {
        // Given
        when(apiKeyRepository.findByTenantId(tenantId)).thenReturn(List.of());

        // When
        boolean result = apiKeyService.validateApiKey(tenantId, plainKey);

        // Then
        assertFalse(result);
    }

    @Test
    void testValidateApiKey_Revoked() {
        // Given
        apiKeyEntity.setRevoked(true);
        when(apiKeyRepository.findByTenantId(tenantId)).thenReturn(List.of(apiKeyEntity));

        // When
        boolean result = apiKeyService.validateApiKey(tenantId, plainKey);

        // Then
        assertFalse(result);
        verify(cryptoService, never()).matches(any(), any());
    }
}
