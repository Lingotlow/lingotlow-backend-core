package com.lingotlow.backendcore.interfaces.api.apikey.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ApiKeyResponse {

    @Schema(description = "API Key unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Timestamp when the API Key was created", example = "2026-01-04T12:30:00Z")
    private OffsetDateTime createdAt;

    @Schema(description = "Indicates whether the API Key is revoked", example = "false")
    private boolean revoked;
}
