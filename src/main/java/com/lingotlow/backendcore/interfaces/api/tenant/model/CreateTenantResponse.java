package com.lingotlow.backendcore.interfaces.api.tenant.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CreateTenantResponse(
    UUID id, String tenantKey, String name, Map<String, Object> config, Instant createdAt) {}
