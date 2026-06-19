package com.lingotlow.backendcore.infrastructure.repository;

import com.lingotlow.backendcore.domain.endpoint.model.Endpoint;
import com.lingotlow.backendcore.domain.tenant.model.Tenant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EndpointRepository extends JpaRepository<Endpoint, UUID> {
    List<Endpoint> findByTenantAndActiveTrue(Tenant tenant);
    Optional<Endpoint> findByTenantAndId(Tenant tenant, UUID id);
}