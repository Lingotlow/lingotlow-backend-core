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

    // Buscar endpoints por tenant
    List<Endpoint> findByTenant(Tenant tenant);

    // Buscar endpoints ativos por tenant
    List<Endpoint> findByTenantAndActiveTrue(Tenant tenant);

    // Buscar endpoint por tenant e ID
    Optional<Endpoint> findByTenantAndId(Tenant tenant, UUID id);

    // Buscar endpoints ativos por tenant para o worker
    List<Endpoint> findByTenantAndActiveTrueOrderByCreatedAtAsc(Tenant tenant);
}