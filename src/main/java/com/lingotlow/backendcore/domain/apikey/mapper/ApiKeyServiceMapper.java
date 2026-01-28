package com.lingotlow.backendcore.domain.apikey.mapper;

import com.lingotlow.backendcore.domain.apikey.model.ApiKeyResponseDTO;
import com.lingotlow.backendcore.infrastructure.repository.entity.ApiKeyEntity;
import org.mapstruct.Mapper;

import java.time.OffsetDateTime;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ApiKeyServiceMapper {
    ApiKeyEntity mapToEntity(UUID id, UUID tenantId, String keyHash, OffsetDateTime createdAt, boolean revoked);

    ApiKeyResponseDTO mapToResponse(ApiKeyEntity apiKey, String plainKey);
}
