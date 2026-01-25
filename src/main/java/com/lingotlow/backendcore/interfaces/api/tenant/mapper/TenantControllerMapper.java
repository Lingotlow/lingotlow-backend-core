package com.lingotlow.backendcore.interfaces.api.tenant.mapper;

import com.lingotlow.backendcore.domain.tenant.model.TenantRequestDTO;
import com.lingotlow.backendcore.domain.tenant.model.TenantUpdateDTO;
import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import com.lingotlow.backendcore.interfaces.api.tenant.model.TenantCreateRequest;
import com.lingotlow.backendcore.interfaces.api.tenant.model.TenantResponse;
import com.lingotlow.backendcore.interfaces.api.tenant.model.TenantSummary;
import com.lingotlow.backendcore.interfaces.api.tenant.model.TenantUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public abstract class TenantControllerMapper {

    public static final TenantControllerMapper INSTANCE = Mappers.getMapper(TenantControllerMapper.class);

    public abstract TenantRequestDTO mapToDomain(TenantCreateRequest request);
    
    public abstract TenantUpdateDTO mapToDomain(TenantUpdateRequest request);
    
    public abstract TenantResponse mapToResponse(TenantEntity entity);
    
    public abstract TenantSummary mapToSummary(TenantEntity entity);
}
