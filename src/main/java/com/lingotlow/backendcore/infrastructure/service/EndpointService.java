package com.lingotlow.backendcore.infrastructure.service;

import com.lingotlow.backendcore.domain.endpoint.model.Endpoint;
import com.lingotlow.backendcore.domain.tenant.model.Tenant;
import com.lingotlow.backendcore.infrastructure.repository.EndpointRepository;
import com.lingotlow.backendcore.infrastructure.repository.TenantRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointService {

    private final EndpointRepository endpointRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public Endpoint createEndpoint(String tenantKey, Endpoint endpoint) {
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

        endpoint.setTenant(tenant);
        endpoint.setStatus("ACTIVE");
        Endpoint saved = endpointRepository.save(endpoint);
        log.info("Endpoint created for tenant: {}, name: {}", tenantKey, endpoint.getName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Endpoint> getEndpointsByTenant(String tenantKey) {
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

        return endpointRepository.findByTenant(tenant);
    }

    @Transactional(readOnly = true)
    public Endpoint getEndpointById(String tenantKey, UUID endpointId) {
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

        return endpointRepository.findByTenantAndId(tenant, endpointId)
                .orElseThrow(() -> new IllegalArgumentException("Endpoint not found: " + endpointId));
    }

    @Transactional
    public Endpoint updateEndpoint(String tenantKey, UUID endpointId, Endpoint updatedEndpoint) {
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

        Endpoint existing = endpointRepository.findByTenantAndId(tenant, endpointId)
                .orElseThrow(() -> new IllegalArgumentException("Endpoint not found: " + endpointId));

        existing.setName(updatedEndpoint.getName());
        existing.setUrl(updatedEndpoint.getUrl());
        existing.setRetryCount(updatedEndpoint.getRetryCount());
        existing.setTimeoutMs(updatedEndpoint.getTimeoutMs());
        existing.setSecret(updatedEndpoint.getSecret());
        existing.setActive(updatedEndpoint.getActive());
        existing.setDescription(updatedEndpoint.getDescription());

        Endpoint saved = endpointRepository.save(existing);
        log.info("Endpoint updated for tenant: {}, id: {}", tenantKey, endpointId);
        return saved;
    }

    @Transactional
    public void deleteEndpoint(String tenantKey, UUID endpointId) {
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantKey));

        Endpoint endpoint = endpointRepository.findByTenantAndId(tenant, endpointId)
                .orElseThrow(() -> new IllegalArgumentException("Endpoint not found: " + endpointId));

        endpoint.setActive(false);
        endpoint.setStatus("INACTIVE");
        endpointRepository.save(endpoint);
        log.info("Endpoint deactivated for tenant: {}, id: {}", tenantKey, endpointId);
    }
}