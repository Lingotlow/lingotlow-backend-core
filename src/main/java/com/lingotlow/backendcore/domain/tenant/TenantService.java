package com.lingotlow.backendcore.domain.tenant;

import com.lingotlow.backendcore.domain.tenant.mapper.TenantServiceMapper;
import com.lingotlow.backendcore.domain.tenant.model.TenantRequestDTO;
import com.lingotlow.backendcore.infrastructure.repository.TenantRepository;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantServiceMapper entityMapper;

    public TenantService(TenantRepository tenantRepository, TenantServiceMapper entityMapper) {
        this.tenantRepository = tenantRepository;
        this.entityMapper = entityMapper;
    }

    public void createTenant(TenantRequestDTO request) {
        if (tenantRepository.existsByTenantKey(request.getTenantKey())) {
            throw new IllegalArgumentException("tenantKey already exist");
        }

        tenantRepository.save(entityMapper.mapToEntity(request));
    }
}
