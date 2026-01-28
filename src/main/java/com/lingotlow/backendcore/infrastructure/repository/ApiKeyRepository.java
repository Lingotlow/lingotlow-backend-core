package com.lingotlow.backendcore.infrastructure.repository;

import com.lingotlow.backendcore.infrastructure.repository.entity.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    @Query("SELECT ak FROM ApiKeyEntity ak WHERE ak.tenantId = :tenantId AND ak.revoked = false ORDER BY ak.createdAt DESC")
    List<ApiKeyEntity> findAllActiveByTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT ak FROM ApiKeyEntity ak WHERE ak.tenantId = :tenantId")
    List<ApiKeyEntity> findByTenantId(@Param("tenantId") UUID tenantId);
}
