package com.lingotlow.backendcore.domain.tenant.mapper;

import com.lingotlow.backendcore.domain.tenant.model.TenantDTO;
import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import org.mapstruct.Mapper;

@Mapper
public abstract class TenantDtoToEntityMapper {

  public abstract TenantEntity mapToEntity(TenantDTO request);
}
