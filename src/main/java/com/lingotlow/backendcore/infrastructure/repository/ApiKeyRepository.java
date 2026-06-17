package com.lingotlow.backendcore.infrastructure.repository;

import com.lingotlow.backendcore.domain.apikey.model.ApiKey;
import com.lingotlow.backendcore.domain.tenant.model.Tenant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByKeyHash(String keyHash);
    List<ApiKey> findByTenantAndRevokedFalse(Tenant tenant);
    boolean existsByTenantAndKeyHash(Tenant tenant, String keyHash);
}