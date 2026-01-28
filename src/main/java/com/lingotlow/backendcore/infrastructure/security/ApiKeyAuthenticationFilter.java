package com.lingotlow.backendcore.infrastructure.security;

import com.lingotlow.backendcore.domain.apikey.ApiKeyService;
import com.lingotlow.backendcore.domain.tenant.TenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private final TenantService tenantService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = extractApiKey(request);
        String tenantKey = extractTenantKey(request);

        if (apiKey != null && tenantKey != null) {
            try {
                UUID tenantId = tenantService.getTenantByTenantKey(tenantKey).getId();

                if (apiKeyService.validateApiKey(tenantId, apiKey)) {
                    log.debug("API key validation successful for tenant: {}", tenantKey);

                    ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(tenantId, apiKey);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("Invalid API key for tenant: {}", tenantKey);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Invalid API key\"}");
                    return;
                }
            } catch (Exception e) {
                log.error("Error validating API key for tenant: {}", tenantKey, e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Authentication failed\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null) {
            apiKey = request.getParameter("api_key");
        }
        return apiKey;
    }

    private String extractTenantKey(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/tenants/")) {
            String[] pathParts = path.split("/");
            if (pathParts.length >= 4) {
                return pathParts[3];
            }
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/");
    }
}
