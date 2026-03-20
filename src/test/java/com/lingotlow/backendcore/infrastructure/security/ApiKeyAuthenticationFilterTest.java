package com.lingotlow.backendcore.infrastructure.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lingotlow.backendcore.domain.apikey.ApiKeyService;
import com.lingotlow.backendcore.domain.tenant.TenantService;
import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private TenantService tenantService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter printWriter;

    @InjectMocks
    private ApiKeyAuthenticationFilter filter;

    private TenantEntity tenantEntity;
    private UUID tenantId;
    private String tenantKey;
    private String apiKey;

    @BeforeEach
    void setUp() throws IOException {
        SecurityContextHolder.clearContext();
        tenantId = UUID.randomUUID();
        tenantKey = "test-tenant";
        apiKey = "lt_test123";

        tenantEntity = new TenantEntity();
        tenantEntity.setId(tenantId);
        tenantEntity.setTenantKey(tenantKey);
        
        // Mock PrintWriter with proper setup
        lenient().when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    void doFilterInternal_ValidApiKey_SetsAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/tenants/test-tenant/some-endpoint");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(tenantService.getTenantByTenantKey(tenantKey)).thenReturn(tenantEntity);
        when(apiKeyService.validateApiKey(tenantId, apiKey)).thenReturn(true);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
        verify(response, never()).getWriter();
        assert SecurityContextHolder.getContext().getAuthentication() != null;
        assert SecurityContextHolder.getContext().getAuthentication().isAuthenticated();
    }

    @Test
    void doFilterInternal_InvalidApiKey_ReturnsUnauthorized() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/tenants/test-tenant/some-endpoint");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(tenantService.getTenantByTenantKey(tenantKey)).thenReturn(tenantEntity);
        when(apiKeyService.validateApiKey(tenantId, apiKey)).thenReturn(false);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(printWriter).write("{\"error\":\"Invalid API key\"}");
        verify(filterChain, never()).doFilter(any(), any());
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }

    @Test
    void doFilterInternal_NoApiKey_SkipsAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/tenants/test-tenant/some-endpoint");
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getParameter("api_key")).thenReturn(null);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }

    @Test
    void doFilterInternal_PublicEndpoint_SkipsFilter() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(tenantService, never()).getTenantByTenantKey(anyString());
        verify(apiKeyService, never()).validateApiKey(any(), anyString());
    }

    @Test
    void doFilterInternal_ApiKeyInParameter_Works() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/tenants/test-tenant/some-endpoint");
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getParameter("api_key")).thenReturn(apiKey);
        when(tenantService.getTenantByTenantKey(tenantKey)).thenReturn(tenantEntity);
        when(apiKeyService.validateApiKey(tenantId, apiKey)).thenReturn(true);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assert SecurityContextHolder.getContext().getAuthentication() != null;
    }

    @Test
    void doFilterInternal_TenantNotFound_ReturnsUnauthorized() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/tenants/test-tenant/some-endpoint");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(tenantService.getTenantByTenantKey(tenantKey))
                .thenThrow(new RuntimeException("Tenant not found"));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(printWriter).write("{\"error\":\"Authentication failed\"}");
        verify(filterChain, never()).doFilter(any(), any());
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }

    @Test
    void doFilterInternal_ApiKeyValidationException_ReturnsUnauthorized() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/tenants/test-tenant/some-endpoint");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(tenantService.getTenantByTenantKey(tenantKey)).thenReturn(tenantEntity);
        when(apiKeyService.validateApiKey(tenantId, apiKey))
                .thenThrow(new RuntimeException("Database error"));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(printWriter).write("{\"error\":\"Authentication failed\"}");
        verify(filterChain, never()).doFilter(any(), any());
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }

    @Test
    void doFilterInternal_EmptyApiKey_SkipsAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/tenants/test-tenant/some-endpoint");
        when(request.getHeader("X-API-Key")).thenReturn("");
        when(request.getParameter("api_key")).thenReturn(null);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }

    @Test
    void doFilterInternal_EmptyTenantKey_SkipsAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/tenants//some-endpoint");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }

    @Test
    void doFilterInternal_InvalidPathFormat_SkipsAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/invalid-path");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }

    @Test
    void doFilterInternal_ActuatorEndpoint_SkipsFilter() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(tenantService, never()).getTenantByTenantKey(anyString());
        verify(apiKeyService, never()).validateApiKey(any(), anyString());
    }

    @Test
    void doFilterInternal_SwaggerEndpoint_SkipsFilter() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(tenantService, never()).getTenantByTenantKey(anyString());
        verify(apiKeyService, never()).validateApiKey(any(), anyString());
    }

    @Test
    void doFilterInternal_ApiDocsEndpoint_SkipsFilter() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/v3/api-docs/openapi.yaml");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(tenantService, never()).getTenantByTenantKey(anyString());
        verify(apiKeyService, never()).validateApiKey(any(), anyString());
    }

    @Test
    void doFilterInternal_ResponseWriterException_HandlesGracefully() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/tenants/test-tenant/some-endpoint");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(tenantService.getTenantByTenantKey(tenantKey)).thenReturn(tenantEntity);
        when(apiKeyService.validateApiKey(tenantId, apiKey)).thenReturn(false);
        when(response.getWriter()).thenThrow(new IOException("Writer error"));

        // When & Then
        assertThatCode(() -> 
                filter.doFilterInternal(request, response, filterChain))
                .doesNotThrowAnyException();
    }
}
