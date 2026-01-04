package com.lingotlow.backendcore.interfaces.api.tenant.mapper;

import com.lingotlow.backendcore.domain.tenant.model.TenantDTO;
import com.lingotlow.backendcore.interfaces.api.tenant.model.TenantCreateRequest;
import org.mapstruct.Mapper;

@Mapper
public abstract class TenantCreateRequestToDtoMapper {

  public abstract TenantDTO mapToDomain(TenantCreateRequest request);
}
