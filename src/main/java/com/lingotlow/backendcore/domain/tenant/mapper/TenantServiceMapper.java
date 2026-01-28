package com.lingotlow.backendcore.domain.tenant.mapper;

import com.lingotlow.backendcore.domain.tenant.model.TenantRequestDTO;
import com.lingotlow.backendcore.domain.tenant.model.TenantUpdateDTO;
import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public abstract class TenantServiceMapper {

  public static final TenantServiceMapper INSTANCE = Mappers.getMapper(TenantServiceMapper.class);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  public abstract TenantEntity mapToCreateEntity(TenantRequestDTO request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "tenantKey", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  public abstract void mapToUpdateEntity(TenantUpdateDTO updateDTO, @MappingTarget TenantEntity entity);
}
