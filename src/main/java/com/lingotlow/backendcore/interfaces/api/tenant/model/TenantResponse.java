package com.lingotlow.backendcore.interfaces.api.tenant.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Complete tenant details")
public class TenantResponse {

  @Schema(
      description = "Tenant unique identifier",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID id;

  @Schema(description = "Tenant unique key", example = "acme-corp")
  private String tenantKey;

  @Schema(description = "Tenant display name", example = "ACME Corporation")
  private String name;

  @Schema(
      description = "Tenant configuration in JSON format",
      example = "{\"webhook_url\": \"https://example.com/webhook\"}")
  private String config;

  @Schema(description = "Timestamp when tenant was created", example = "2026-01-04T12:30:00Z")
  private OffsetDateTime createdAt;

  @Schema(description = "Timestamp when tenant was last updated", example = "2026-01-04T12:30:00Z")
  private OffsetDateTime updatedAt;
}
