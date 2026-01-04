package com.lingotlow.backendcore.interfaces.api.tenant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TenantCreateRequest {

  @NotBlank
  @Pattern(regexp = "^[a-zA-Z0-9]+$")
  private String tenantKey;

  @NotBlank private String name;

  private String config;
}
