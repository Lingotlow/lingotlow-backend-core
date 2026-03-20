package com.lingotlow.backendcore.interfaces.api.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    @WithMockUser(roles = {"ADMIN"})
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
    @WithMockUser(roles = {"ADMIN"})
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
    @WithMockUser(roles = {"USER"}) // Not ADMIN
    @DisplayName("Should return 403 when creating tenant without ADMIN role")
    void createTenant_ReturnsForbidden_WhenNotAdmin() throws Exception {
        // Given
        TenantCreateRequest request = new TenantCreateRequest();
        request.setTenantKey(tenantKey);
        request.setName("Test Tenant");

        // When & Then
        mockMvc.perform(post("/api/tenants")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(tenantService, never()).createTenant(any());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
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
    @WithMockUser(roles = {"USER"}) // Not ADMIN
    @DisplayName("Should return 403 when listing tenants without ADMIN role")
    void listTenants_ReturnsForbidden_WhenNotAdmin() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isForbidden());

        verify(tenantService, never()).listTenants(any());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
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
    @WithMockUser(roles = {"ADMIN"})
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
    @WithMockUser(roles = {"ADMIN"})
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
    @WithMockUser(roles = {"ADMIN"})
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
    @WithMockUser(roles = {"ADMIN"})
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
    @WithMockUser(roles = {"ADMIN"})
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
    @DisplayName("Should return 403 when accessing without authentication")
    void endpoints_ReturnsForbidden_WhenNotAuthenticated() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tenants/{tenantKey}", tenantKey))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());

        verify(tenantService, never()).listTenants(any());
        verify(tenantService, never()).getTenantByTenantKey(any());
        verify(tenantService, never()).createTenant(any());
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
