package com.lingotlow.backendcore.interfaces.api.tenant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

public record CreateTenantRequest(

    @NotBlank
    @Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "tenantKey must be alphanumeric"
    )
    String tenantKey,

    @NotBlank
    String name,

    Map<String, Object> config
) {}
