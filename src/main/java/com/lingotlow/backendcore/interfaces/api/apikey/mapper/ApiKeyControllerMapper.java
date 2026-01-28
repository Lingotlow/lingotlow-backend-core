package com.lingotlow.backendcore.interfaces.api.apikey.mapper;

import com.lingotlow.backendcore.infrastructure.repository.entity.ApiKeyEntity;
import com.lingotlow.backendcore.interfaces.api.apikey.model.ApiKeyResponse;
import com.lingotlow.backendcore.interfaces.api.apikey.model.CreateApiKeyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApiKeyControllerMapper {
    ApiKeyResponse toApiKeyResponseDTO(ApiKeyEntity apiKey);

    @Mapping(target = "id", source = "apiKey.id")
    @Mapping(target = "apiKey", source = "plainKey")
    @Mapping(target = "createdAt", source = "apiKey.createdAt")
    CreateApiKeyResponse toCreateApiKeyResponseDTO(ApiKeyEntity apiKey, String plainKey);
}
