package com.lingotlow.backendcore.domain.tenant.model;

import lombok.Data;

@Data
public class TenantRequestDTO {
    private final String tenantKey;
    private String name;
    private String config;
}
