package com.lingotlow.backendcore.infrastructure.repository;

import com.lingotlow.backendcore.infrastructure.repository.entity.ApiKeyEntity;
import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ApiKeyRepository Integration Tests")
class ApiKeyRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantId;
    private UUID tenant2Id;

    @BeforeEach
    void setUp() {
        // Clean up
        apiKeyRepository.deleteAll();
        tenantRepository.deleteAll();
        
        // Create test tenants
        TenantEntity tenant = new TenantEntity();
        tenant.setTenantKey("test-tenant");
        tenant.setName("Test Tenant");
        tenant.setConfig("{\"test\": true}");
        tenant.setCreatedAt(OffsetDateTime.now());
        tenant.setUpdatedAt(OffsetDateTime.now());
        
        TenantEntity tenant2 = new TenantEntity();
        tenant2.setTenantKey("test-tenant-2");
        tenant2.setName("Test Tenant 2");
        tenant2.setConfig("{\"test\": true}");
        tenant2.setCreatedAt(OffsetDateTime.now());
        tenant2.setUpdatedAt(OffsetDateTime.now());
        
        tenant = entityManager.persistAndFlush(tenant);
        tenant2 = entityManager.persistAndFlush(tenant2);
        
        tenantId = tenant.getId();
        tenant2Id = tenant2.getId();
    }

    @Test
    @DisplayName("Should find all active API keys for tenant")
    void findAllActiveByTenant_Success() {
        // Given
        ApiKeyEntity activeKey1 = createSampleApiKey(tenantId, false);
        ApiKeyEntity activeKey2 = createSampleApiKey(tenantId, false);
        ApiKeyEntity revokedKey = createSampleApiKey(tenantId, true);
        ApiKeyEntity otherTenantKey = createSampleApiKey(tenant2Id, false);
        
        entityManager.persistAndFlush(activeKey1);
        entityManager.persistAndFlush(activeKey2);
        entityManager.persistAndFlush(revokedKey);
        entityManager.persistAndFlush(otherTenantKey);

        // When
        List<ApiKeyEntity> result = apiKeyRepository.findAllActiveByTenant(tenantId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ApiKeyEntity::getId)
                .containsExactlyInAnyOrder(activeKey1.getId(), activeKey2.getId());
        assertThat(result).allMatch(key -> !key.isRevoked());
    }

    @Test
    @DisplayName("Should return empty list when no active API keys for tenant")
    void findAllActiveByTenant_ReturnsEmpty_WhenNoActiveKeys() {
        // Given
        ApiKeyEntity revokedKey = createSampleApiKey(tenantId, true);
        entityManager.persistAndFlush(revokedKey);

        // When
        List<ApiKeyEntity> result = apiKeyRepository.findAllActiveByTenant(tenantId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when tenant has no API keys")
    void findAllActiveByTenant_ReturnsEmpty_WhenTenantHasNoKeys() {
        // When
        List<ApiKeyEntity> result = apiKeyRepository.findAllActiveByTenant(tenantId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find all API keys for tenant including revoked")
    void findByTenantId_Success() {
        // Given
        ApiKeyEntity activeKey = createSampleApiKey(tenantId, false);
        ApiKeyEntity revokedKey = createSampleApiKey(tenantId, true);
        ApiKeyEntity otherTenantKey = createSampleApiKey(tenant2Id, false);
        
        entityManager.persistAndFlush(activeKey);
        entityManager.persistAndFlush(revokedKey);
        entityManager.persistAndFlush(otherTenantKey);

        // When
        List<ApiKeyEntity> result = apiKeyRepository.findByTenantId(tenantId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ApiKeyEntity::getId)
                .containsExactlyInAnyOrder(activeKey.getId(), revokedKey.getId());
    }

    @Test
    @DisplayName("Should return empty list when tenant has no API keys at all")
    void findByTenantId_ReturnsEmpty_WhenTenantHasNoKeys() {
        // When
        List<ApiKeyEntity> result = apiKeyRepository.findByTenantId(tenantId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple API keys with different creation times")
    void findAllActiveByTenant_OrdersByCreatedAtDesc() {
        // Given
        OffsetDateTime time1 = OffsetDateTime.now().minusHours(2);
        OffsetDateTime time2 = OffsetDateTime.now().minusHours(1);
        OffsetDateTime time3 = OffsetDateTime.now();
        
        ApiKeyEntity key1 = createSampleApiKeyWithCreatedAt(tenantId, false, time1);
        ApiKeyEntity key2 = createSampleApiKeyWithCreatedAt(tenantId, false, time2);
        ApiKeyEntity key3 = createSampleApiKeyWithCreatedAt(tenantId, false, time3);
        
        entityManager.persistAndFlush(key1);
        entityManager.persistAndFlush(key2);
        entityManager.persistAndFlush(key3);

        // When
        List<ApiKeyEntity> result = apiKeyRepository.findAllActiveByTenant(tenantId);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(ApiKeyEntity::getCreatedAt)
                .containsExactly(time3, time2, time1); // Should be in descending order
    }

    @Test
    @DisplayName("Should handle tenant isolation correctly")
    void findAllActiveByTenant_EnforcesTenantIsolation() {
        // Given
        ApiKeyEntity tenant1Key = createSampleApiKey(tenantId, false);
        ApiKeyEntity tenant2Key = createSampleApiKey(tenant2Id, false);
        
        entityManager.persistAndFlush(tenant1Key);
        entityManager.persistAndFlush(tenant2Key);

        // When
        List<ApiKeyEntity> result1 = apiKeyRepository.findAllActiveByTenant(tenantId);
        List<ApiKeyEntity> result2 = apiKeyRepository.findAllActiveByTenant(tenant2Id);

        // Then
        assertThat(result1).hasSize(1);
        assertThat(result2).hasSize(1);
        
        assertThat(result1.get(0).getId()).isEqualTo(tenant1Key.getId());
        assertThat(result2.get(0).getId()).isEqualTo(tenant2Key.getId());
    }

    // Helper methods
    private ApiKeyEntity createSampleApiKey(UUID tId, boolean revoked) {
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setTenantId(tId);
        apiKey.setKeyHash("hashed_key_" + UUID.randomUUID());
        apiKey.setCreatedAt(OffsetDateTime.now());
        apiKey.setRevoked(revoked);
        return apiKey;
    }

    private ApiKeyEntity createSampleApiKeyWithCreatedAt(UUID tId, boolean revoked, OffsetDateTime createdAt) {
        ApiKeyEntity apiKey = createSampleApiKey(tId, revoked);
        apiKey.setCreatedAt(createdAt);
        return apiKey;
    }
}
