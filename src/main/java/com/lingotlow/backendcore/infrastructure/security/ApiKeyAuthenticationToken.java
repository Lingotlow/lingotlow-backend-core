package com.lingotlow.backendcore.infrastructure.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final UUID tenantId;
    private final String apiKey;

    public ApiKeyAuthenticationToken(UUID tenantId, String apiKey) {
        super(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        this.tenantId = tenantId;
        this.apiKey = apiKey;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return tenantId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getApiKey() {
        return apiKey;
    }
}
