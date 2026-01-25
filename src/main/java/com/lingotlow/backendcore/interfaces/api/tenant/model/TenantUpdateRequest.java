package com.lingotlow.backendcore.interfaces.api.tenant.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantUpdateRequest {

  @NotBlank private String name;

  private String config;
}
