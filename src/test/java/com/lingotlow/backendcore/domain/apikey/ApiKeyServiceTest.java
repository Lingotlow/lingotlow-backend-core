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

    @Test
    void testValidateApiKey_MultipleTenants_Isolation() {
        // Given
        UUID tenant1Id = UUID.randomUUID();
        UUID tenant2Id = UUID.randomUUID();
        
        ApiKeyEntity tenant1Key = new ApiKeyEntity();
        tenant1Key.setId(UUID.randomUUID());
        tenant1Key.setTenantId(tenant1Id);
        tenant1Key.setKeyHash(hashedKey);
        tenant1Key.setCreatedAt(OffsetDateTime.now());
        tenant1Key.setRevoked(false);

        ApiKeyEntity tenant2Key = new ApiKeyEntity();
        tenant2Key.setId(UUID.randomUUID());
        tenant2Key.setTenantId(tenant2Id);
        tenant2Key.setKeyHash("different_hash");
        tenant2Key.setCreatedAt(OffsetDateTime.now());
        tenant2Key.setRevoked(false);

        when(apiKeyRepository.findByTenantId(tenant1Id)).thenReturn(List.of(tenant1Key));
        when(apiKeyRepository.findByTenantId(tenant2Id)).thenReturn(List.of(tenant2Key));
        when(cryptoService.matches(plainKey, hashedKey)).thenReturn(true);
        when(cryptoService.matches(plainKey, "different_hash")).thenReturn(false);

        // When
        boolean result1 = apiKeyService.validateApiKey(tenant1Id, plainKey);
        boolean result2 = apiKeyService.validateApiKey(tenant2Id, plainKey);

        // Then
        assertTrue(result1);
        assertFalse(result2);
        verify(cryptoService).matches(plainKey, hashedKey);
        verify(cryptoService).matches(plainKey, "different_hash");
    }

    @Test
    void testValidateApiKey_MultipleKeysForSameTenant() {
        // Given
        ApiKeyEntity key1 = new ApiKeyEntity();
        key1.setId(UUID.randomUUID());
        key1.setTenantId(tenantId);
        key1.setKeyHash("hash1");
        key1.setCreatedAt(OffsetDateTime.now());
        key1.setRevoked(false);

        ApiKeyEntity key2 = new ApiKeyEntity();
        key2.setId(UUID.randomUUID());
        key2.setTenantId(tenantId);
        key2.setKeyHash(hashedKey);
        key2.setCreatedAt(OffsetDateTime.now());
        key2.setRevoked(false);

        when(apiKeyRepository.findByTenantId(tenantId)).thenReturn(List.of(key1, key2));
        when(cryptoService.matches(plainKey, "hash1")).thenReturn(false);
        when(cryptoService.matches(plainKey, hashedKey)).thenReturn(true);

        // When
        boolean result = apiKeyService.validateApiKey(tenantId, plainKey);

        // Then
        assertTrue(result);
        verify(cryptoService).matches(plainKey, "hash1");
        verify(cryptoService).matches(plainKey, hashedKey);
    }

    @Test
    void testValidateApiKey_NullKey() {
        // Given
        lenient().when(apiKeyRepository.findByTenantId(tenantId)).thenReturn(List.of(apiKeyEntity));

        // When
        boolean result = apiKeyService.validateApiKey(tenantId, null);

        // Then
        assertFalse(result);
        verify(cryptoService, never()).matches(any(), any());
    }

    @Test
    void testValidateApiKey_EmptyKey() {
        // Given
        lenient().when(apiKeyRepository.findByTenantId(tenantId)).thenReturn(List.of(apiKeyEntity));

        // When
        boolean result = apiKeyService.validateApiKey(tenantId, "");

        // Then
        assertFalse(result);
        verify(cryptoService, never()).matches(any(), any());
    }
}
