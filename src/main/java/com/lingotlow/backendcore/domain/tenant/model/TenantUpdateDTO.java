package com.lingotlow.backendcore.domain.tenant.model;

import lombok.Data;

@Data
public class TenantUpdateDTO {
  private String name;
  private String config;
}
