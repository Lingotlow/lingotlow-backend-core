package com.lingotlow.backendcore.domain.apikey.model;

import com.lingotlow.backendcore.infrastructure.repository.entity.ApiKeyEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiKeyResponseDTO {
    private ApiKeyEntity apiKey;
    private String plainKey;
}
