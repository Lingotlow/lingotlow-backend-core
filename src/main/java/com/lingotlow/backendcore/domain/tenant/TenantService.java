package com.lingotlow.backendcore.domain.tenant;

import com.lingotlow.backendcore.domain.tenant.mapper.TenantDtoToEntityMapper;
import com.lingotlow.backendcore.domain.tenant.model.TenantDTO;
import com.lingotlow.backendcore.infrastructure.repository.TenantRepository;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

  private final TenantRepository tenantRepository;
  private final TenantDtoToEntityMapper entityMapper;

  public TenantService(TenantRepository tenantRepository, TenantDtoToEntityMapper entityMapper) {
    this.tenantRepository = tenantRepository;
    this.entityMapper = entityMapper;
  }

  public void createTenant(TenantDTO request) {
    if (tenantRepository.existsByTenantKey(request.getTenantKey())) {
      throw new IllegalArgumentException("tenantKey already exist");
    }

    tenantRepository.save(entityMapper.mapToEntity(request));
  }
}
