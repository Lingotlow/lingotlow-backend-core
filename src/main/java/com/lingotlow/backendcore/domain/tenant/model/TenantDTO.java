package com.lingotlow.backendcore.domain.tenant.model;

import lombok.Data;

@Data
public class TenantDTO {
  private final String tenantKey;
  private String name;
  private String config;
}
