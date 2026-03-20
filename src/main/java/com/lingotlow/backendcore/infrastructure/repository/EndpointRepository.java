package com.lingotlow.backendcore.infrastructure.repository;

import com.lingotlow.backendcore.infrastructure.repository.entity.EndpointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EndpointRepository extends JpaRepository<EndpointEntity, UUID> {

    @Query("SELECT e FROM EndpointEntity e WHERE e.tenantId = :tenantId AND e.active = true ORDER BY e.name ASC")
    List<EndpointEntity> findAllActiveByTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT e FROM EndpointEntity e WHERE e.tenantId = :tenantId ORDER BY e.name ASC")
    List<EndpointEntity> findAllByTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT e FROM EndpointEntity e WHERE e.tenantId = :tenantId AND e.id = :endpointId AND e.active = true")
    Optional<EndpointEntity> findActiveByTenantAndId(@Param("tenantId") UUID tenantId, @Param("endpointId") UUID endpointId);

    boolean existsByTenantIdAndName(UUID tenantId, String name);

    @Query("SELECT COUNT(e) FROM EndpointEntity e WHERE e.tenantId = :tenantId AND e.active = true")
    long countActiveByTenant(@Param("tenantId") UUID tenantId);
}
