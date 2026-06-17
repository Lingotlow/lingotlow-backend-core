package com.lingotlow.backendcore.infrastructure.security;

import com.lingotlow.backendcore.domain.apikey.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        log.debug("🔑 ApiKeyAuthenticationFilter processing: {}", path);

        // Skip API Key authentication for login endpoint
        if (path.contains("/api/auth/login")) {
            log.debug("⏭️ Skipping API Key filter for login endpoint");
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-API-Key");

        if (StringUtils.hasText(apiKey) && isIngestionEndpoint(request)) {
            try {
                String tenantKey = extractTenantKey(request);
                if (tenantKey != null && apiKeyService.validateApiKey(tenantKey, apiKey)) {
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            tenantKey, null, List.of()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("✅ API Key authenticated for tenant: {}", tenantKey);
                } else {
                    log.warn("❌ Invalid API Key for tenant: {}", tenantKey);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Invalid API Key\"}");
                    return;
                }
            } catch (Exception e) {
                log.error("❌ Error validating API Key", e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Authentication failed\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isIngestionEndpoint(HttpServletRequest request) {
        return request.getRequestURI().contains("/api/ingest/");
    }

    private String extractTenantKey(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.contains("/api/ingest/")) {
            String[] parts = path.split("/api/ingest/");
            if (parts.length > 1) {
                return parts[1].split("/")[0];
            }
        }
        return null;
    }
}