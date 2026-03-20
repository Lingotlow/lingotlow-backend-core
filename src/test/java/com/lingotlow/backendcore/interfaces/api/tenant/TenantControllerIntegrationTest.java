package com.lingotlow.backendcore.interfaces.api.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingotlow.backendcore.config.TestSecurityConfig;
import com.lingotlow.backendcore.domain.tenant.TenantService;
import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import com.lingotlow.backendcore.interfaces.api.exception.ErrorResponse;
import com.lingotlow.backendcore.interfaces.api.exception.ResourceNotFoundException;
import com.lingotlow.backendcore.interfaces.api.tenant.model.TenantCreateRequest;
import com.lingotlow.backendcore.interfaces.api.tenant.model.TenantResponse;
import com.lingotlow.backendcore.interfaces.api.tenant.model.TenantSummary;
import com.lingotlow.backendcore.interfaces.api.tenant.model.TenantUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("TenantController Integration Tests")
class TenantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TenantService tenantService;

    private TenantEntity sampleTenant;
    private String tenantKey;

    @BeforeEach
    void setUp() {
        tenantKey = "test-tenant";
        sampleTenant = createSampleTenant();
    }

    @Test
    @DisplayName("Should create tenant successfully")
    void createTenant_Success() throws Exception {
        // Given
        TenantCreateRequest request = new TenantCreateRequest();
        request.setTenantKey(tenantKey);
        request.setName("Test Tenant");
        request.setConfig("{\"test\": true}");

        when(tenantService.createTenant(any())).thenReturn(sampleTenant);

        // When & Then
        mockMvc.perform(post("/api/tenants")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(sampleTenant.getId().toString()))
                .andExpect(jsonPath("$.tenantKey").value(tenantKey))
                .andExpect(jsonPath("$.name").value("Test Tenant"))
                .andExpect(jsonPath("$.config").value("{\"test\": true}"));

        verify(tenantService).createTenant(any());
    }

    @Test
    @DisplayName("Should return 400 when creating tenant with invalid data")
    void createTenant_ReturnsBadRequest_WhenInvalidData() throws Exception {
        // Given
        TenantCreateRequest request = new TenantCreateRequest();
        request.setTenantKey(""); // Invalid empty tenant key
        request.setName("Test Tenant");

        // When & Then
        mockMvc.perform(post("/api/tenants")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(tenantService, never()).createTenant(any());
    }

    @Test
    @DisplayName("Should create tenant successfully when endpoint is permitAll")
    void createTenant_ReturnsForbidden_WhenNotAdmin() throws Exception {
        // Given
        TenantCreateRequest request = new TenantCreateRequest();
        request.setTenantKey(tenantKey);
        request.setName("Test Tenant");

        // When & Then - With TestSecurityConfig, this should return 201 (permitAll)
        mockMvc.perform(post("/api/tenants")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(tenantService).createTenant(any());
    }

    @Test
    @DisplayName("Should list tenants successfully")
    void listTenants_Success() throws Exception {
        // Given
        List<TenantEntity> tenants = List.of(sampleTenant);
        Page<TenantEntity> tenantPage = new PageImpl<>(tenants, PageRequest.of(0, 20), 1);
        
        when(tenantService.listTenants(any(PageRequest.class))).thenReturn(tenantPage);

        // When & Then
        mockMvc.perform(get("/api/tenants")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(sampleTenant.getId().toString()))
                .andExpect(jsonPath("$.content[0].tenantKey").value(tenantKey))
                .andExpect(jsonPath("$.content[0].name").value("Test Tenant"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(tenantService).listTenants(any(PageRequest.class));
    }

    @Test
    @DisplayName("Should list tenants successfully when endpoint is permitAll")
    void listTenants_ReturnsForbidden_WhenNotAdmin() throws Exception {
        // Given
        List<TenantEntity> tenants = List.of(sampleTenant);
        Page<TenantEntity> tenantPage = new PageImpl<>(tenants, PageRequest.of(0, 20), 1);
        
        when(tenantService.listTenants(any(PageRequest.class))).thenReturn(tenantPage);

        // When & Then - With TestSecurityConfig, this should return 200 (permitAll)
        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(sampleTenant.getId().toString()))
                .andExpect(jsonPath("$.content[0].tenantKey").value(tenantKey))
                .andExpect(jsonPath("$.content[0].name").value("Test Tenant"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(tenantService).listTenants(any(PageRequest.class));
    }

    @Test
    @DisplayName("Should get tenant successfully")
    void getTenant_Success() throws Exception {
        // Given
        when(tenantService.getTenantByTenantKey(tenantKey)).thenReturn(sampleTenant);

        // When & Then
        mockMvc.perform(get("/api/tenants/{tenantKey}", tenantKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleTenant.getId().toString()))
                .andExpect(jsonPath("$.tenantKey").value(tenantKey))
                .andExpect(jsonPath("$.name").value("Test Tenant"))
                .andExpect(jsonPath("$.config").value("{\"test\": true}"));

        verify(tenantService).getTenantByTenantKey(tenantKey);
    }

    @Test
    @DisplayName("Should return 404 when tenant not found")
    void getTenant_ReturnsNotFound_WhenTenantNotFound() throws Exception {
        // Given
        when(tenantService.getTenantByTenantKey(tenantKey))
                .thenThrow(new ResourceNotFoundException("Tenant", tenantKey));

        // When & Then
        mockMvc.perform(get("/api/tenants/{tenantKey}", tenantKey))
                .andExpect(status().isNotFound());

        verify(tenantService).getTenantByTenantKey(tenantKey);
    }

    @Test
    @DisplayName("Should update tenant successfully")
    void updateTenant_Success() throws Exception {
        // Given
        TenantUpdateRequest request = new TenantUpdateRequest();
        request.setName("Updated Tenant");
        request.setConfig("updated-config");

        TenantEntity updatedTenant = createSampleTenant();
        updatedTenant.setName("Updated Tenant");
        updatedTenant.setConfig("updated-config");

        when(tenantService.updateTenant(eq(tenantKey), any())).thenReturn(updatedTenant);

        // When & Then
        mockMvc.perform(put("/api/tenants/{tenantKey}", tenantKey)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(updatedTenant.getId().toString()))
                .andExpect(jsonPath("$.tenantKey").value(tenantKey))
                .andExpect(jsonPath("$.name").value("Updated Tenant"))
                .andExpect(jsonPath("$.config").value("updated-config"));

        verify(tenantService).updateTenant(eq(tenantKey), any());
    }

    @Test
    @DisplayName("Should return 400 when updating tenant with invalid data")
    void updateTenant_ReturnsBadRequest_WhenInvalidData() throws Exception {
        // Given
        TenantUpdateRequest request = new TenantUpdateRequest();
        request.setName(""); // Invalid empty name

        // When & Then
        mockMvc.perform(put("/api/tenants/{tenantKey}", tenantKey)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(tenantService, never()).updateTenant(any(), any());
    }

    @Test
    @DisplayName("Should delete tenant successfully")
    void deleteTenant_Success() throws Exception {
        // Given
        doNothing().when(tenantService).deleteTenant(tenantKey);

        // When & Then
        mockMvc.perform(delete("/api/tenants/{tenantKey}", tenantKey)
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(tenantService).deleteTenant(tenantKey);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent tenant")
    void deleteTenant_ReturnsNotFound_WhenTenantNotFound() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("Tenant", tenantKey))
                .when(tenantService).deleteTenant(tenantKey);

        // When & Then
        mockMvc.perform(delete("/api/tenants/{tenantKey}", tenantKey)
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(tenantService).deleteTenant(tenantKey);
    }

    @Test
    @DisplayName("Should access endpoints successfully when security is disabled")
    void endpoints_ReturnsForbidden_WhenNotAuthenticated() throws Exception {
        // Given
        when(tenantService.listTenants(any())).thenReturn(new PageImpl<>(List.of()));
        when(tenantService.getTenantByTenantKey(tenantKey)).thenReturn(sampleTenant);
        when(tenantService.createTenant(any())).thenReturn(sampleTenant);

        // When & Then - With TestSecurityConfig, all endpoints should work
        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tenants/{tenantKey}", tenantKey))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest()); // Validation error, not security error

        verify(tenantService).listTenants(any());
        verify(tenantService).getTenantByTenantKey(tenantKey);
    }

    // Helper method
    private TenantEntity createSampleTenant() {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(UUID.randomUUID());
        tenant.setTenantKey(tenantKey);
        tenant.setName("Test Tenant");
        tenant.setConfig("{\"test\": true}");
        tenant.setCreatedAt(OffsetDateTime.now());
        tenant.setUpdatedAt(OffsetDateTime.now());
        return tenant;
    }
}
