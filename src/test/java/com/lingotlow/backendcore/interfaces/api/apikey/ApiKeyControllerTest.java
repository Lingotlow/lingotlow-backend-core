package com.lingotlow.backendcore.interfaces.api.apikey;

import com.lingotlow.backendcore.domain.apikey.ApiKeyService;
import com.lingotlow.backendcore.domain.apikey.model.ApiKeyResponseDTO;
import com.lingotlow.backendcore.domain.tenant.TenantService;
import com.lingotlow.backendcore.infrastructure.repository.entity.ApiKeyEntity;
import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import com.lingotlow.backendcore.interfaces.api.apikey.mapper.ApiKeyControllerMapper;
import com.lingotlow.backendcore.interfaces.api.apikey.model.ApiKeyResponse;
import com.lingotlow.backendcore.interfaces.api.apikey.model.CreateApiKeyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyControllerTest {

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private TenantService tenantService;

    @Mock
    private ApiKeyControllerMapper mapper;

    @InjectMocks
    private ApiKeyController apiKeyController;

    private UUID tenantId;
    private UUID keyId;
    private String tenantKey;
    private String plainApiKey;
    private ApiKeyEntity apiKeyEntity;
    private TenantEntity tenantEntity;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        keyId = UUID.randomUUID();
        tenantKey = "test-tenant";
        plainApiKey = "lt_test123456789";

        tenantEntity = new TenantEntity();
        tenantEntity.setId(tenantId);
        tenantEntity.setTenantKey(tenantKey);
        tenantEntity.setName("Test Tenant");
        tenantEntity.setCreatedAt(OffsetDateTime.now());
        tenantEntity.setUpdatedAt(OffsetDateTime.now());

        apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setId(keyId);
        apiKeyEntity.setTenantId(tenantId);
        apiKeyEntity.setKeyHash("hashed_key");
        apiKeyEntity.setCreatedAt(OffsetDateTime.now());
        apiKeyEntity.setRevoked(false);
    }

    @Test
    void testCreateApiKey_Success() {
        // Given
        ApiKeyResponseDTO serviceResponse = new ApiKeyResponseDTO(apiKeyEntity, plainApiKey);
        CreateApiKeyResponse expectedResponse = new CreateApiKeyResponse(keyId, plainApiKey, apiKeyEntity.getCreatedAt());

        when(tenantService.getTenantByTenantKey(tenantKey)).thenReturn(tenantEntity);
        when(apiKeyService.createApiKey(tenantId)).thenReturn(serviceResponse);
        when(mapper.toCreateApiKeyResponseDTO(apiKeyEntity, plainApiKey)).thenReturn(expectedResponse);

        // When
        ResponseEntity<CreateApiKeyResponse> response = apiKeyController.createApiKey(tenantKey);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(keyId, response.getBody().getId());
        assertEquals(plainApiKey, response.getBody().getApiKey());
        assertEquals(apiKeyEntity.getCreatedAt(), response.getBody().getCreatedAt());
    }

    @Test
    void testListApiKeys_Success() {
        // Given
        List<ApiKeyEntity> apiKeys = List.of(apiKeyEntity);
        ApiKeyResponse expectedResponse = new ApiKeyResponse(keyId, apiKeyEntity.getCreatedAt(), false);

        when(tenantService.getTenantByTenantKey(tenantKey)).thenReturn(tenantEntity);
        when(apiKeyService.listApiKeys(tenantId)).thenReturn(apiKeys);
        when(mapper.toApiKeyResponseDTO(apiKeyEntity)).thenReturn(expectedResponse);

        // When
        ResponseEntity<List<ApiKeyResponse>> response = apiKeyController.listApiKeys(tenantKey);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(keyId, response.getBody().get(0).getId());
        assertEquals(apiKeyEntity.getCreatedAt(), response.getBody().get(0).getCreatedAt());
        assertFalse(response.getBody().get(0).isRevoked());
    }

    @Test
    void testRevokeApiKey_Success() {
        // Given
        when(tenantService.getTenantByTenantKey(tenantKey)).thenReturn(tenantEntity);

        // When
        ResponseEntity<Void> response = apiKeyController.revokeApiKey(tenantKey, keyId);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void testCreateApiKey_TenantNotFound() {
        // Given
        when(tenantService.getTenantByTenantKey(tenantKey))
                .thenThrow(new RuntimeException("Tenant não encontrado"));

        // When & Then
        assertThrows(RuntimeException.class, () -> apiKeyController.createApiKey(tenantKey));
    }

    @Test
    void testListApiKeys_TenantNotFound() {
        // Given
        when(tenantService.getTenantByTenantKey(tenantKey))
                .thenThrow(new RuntimeException("Tenant não encontrado"));

        // When & Then
        assertThrows(RuntimeException.class, () -> apiKeyController.listApiKeys(tenantKey));
    }

    @Test
    void testRevokeApiKey_TenantNotFound() {
        // Given
        when(tenantService.getTenantByTenantKey(tenantKey))
                .thenThrow(new RuntimeException("Tenant não encontrado"));

        // When & Then
        assertThrows(RuntimeException.class, () -> apiKeyController.revokeApiKey(tenantKey, keyId));
    }

    @Test
    void testRevokeApiKey_ApiKeyNotFound() {
        // Given
        when(tenantService.getTenantByTenantKey(tenantKey)).thenReturn(tenantEntity);
        doThrow(new RuntimeException("API Key não encontrada"))
                .when(apiKeyService).revokeApiKey(tenantId, keyId);

        // When & Then
        assertThrows(RuntimeException.class, () -> apiKeyController.revokeApiKey(tenantKey, keyId));
    }
}
