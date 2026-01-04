package com.lingotlow.backendcore.interfaces.api.tenant.mapper;

import com.lingotlow.backendcore.domain.tenant.model.TenantRequestDTO;
import com.lingotlow.backendcore.interfaces.api.tenant.model.TenantCreateRequest;
import org.mapstruct.Mapper;

@Mapper
public abstract class TenantControllerMapper {

    public abstract TenantRequestDTO mapToDomain(TenantCreateRequest request);
}
