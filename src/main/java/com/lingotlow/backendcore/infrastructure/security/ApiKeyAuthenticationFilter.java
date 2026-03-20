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
        
        log.debug("Processing request: path={}, apiKey={}, tenantKey={}", 
                 request.getRequestURI(), 
                 apiKey != null ? "[PRESENT]" : "[NULL]", 
                 tenantKey != null ? tenantKey : "[NULL]");

        if (apiKey != null) {
            try {
                // For endpoints that don't require tenantKey in URL (like GET /api/tenants)
                // we need to handle authentication differently
                if (tenantKey == null && (request.getRequestURI().equals("/api/tenants") && 
                    ("GET".equals(request.getMethod()) || "POST".equals(request.getMethod())))) {
                    // For tenant listing/creation, we'll need to validate differently
                    // For now, let the request proceed and handle authorization in controller
                    log.info("Processing tenant management endpoint without tenant context");
                    filterChain.doFilter(request, response);
                    return;
                } else if (tenantKey != null) {
                    UUID tenantId = tenantService.getTenantByTenantKey(tenantKey).getId();
                    log.info("Found tenantId: {} for tenantKey: {}", tenantId, tenantKey);

                    if (apiKeyService.validateApiKey(tenantId, apiKey)) {
                        log.info("API key validation successful for tenant: {}", tenantKey);

                        ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(tenantId, apiKey);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Authentication set for tenant: {} with API key", tenantKey);
                    } else {
                        log.warn("Invalid API key for tenant: {}", tenantKey);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        try {
                            response.getWriter().write("{\"error\":\"Invalid API key\"}");
                        } catch (IOException ioException) {
                            log.error("Failed to write error response", ioException);
                        }
                        return;
                    }
                } else {
                    log.warn("API key present but no tenant key found for path: {}", request.getRequestURI());
                }
            } catch (Exception e) {
                log.error("Error validating API key for tenant: {}", tenantKey, e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                try {
                    response.getWriter().write("{\"error\":\"Authentication failed\"}");
                } catch (IOException ioException) {
                    log.error("Failed to write error response", ioException);
                }
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = request.getParameter("api_key");
        }
        return (apiKey != null && !apiKey.trim().isEmpty()) ? apiKey : null;
    }

    private String extractTenantKey(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/tenants/")) {
            String[] pathParts = path.split("/");
            if (pathParts.length >= 4) {
                String tenantKey = pathParts[3];
                return (tenantKey != null && !tenantKey.trim().isEmpty()) ? tenantKey : null;
            }
        } else if (path.startsWith("/api/ingest/")) {
            String[] pathParts = path.split("/");
            if (pathParts.length >= 4) {
                String tenantKey = pathParts[3];
                return (tenantKey != null && !tenantKey.trim().isEmpty()) ? tenantKey : null;
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
