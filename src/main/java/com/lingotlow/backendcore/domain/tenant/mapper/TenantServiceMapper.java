package com.lingotlow.backendcore.domain.tenant.mapper;

import com.lingotlow.backendcore.domain.tenant.model.TenantRequestDTO;
import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import org.mapstruct.Mapper;

@Mapper
public abstract class TenantServiceMapper {

    public abstract TenantEntity mapToEntity(TenantRequestDTO request);
}
