package com.lingotlow.backendcore.domain.tenant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lingotlow.backendcore.domain.tenant.mapper.TenantServiceMapper;
import com.lingotlow.backendcore.domain.tenant.model.TenantRequestDTO;
import com.lingotlow.backendcore.domain.tenant.model.TenantUpdateDTO;
import com.lingotlow.backendcore.infrastructure.logging.AuditLogger;
import com.lingotlow.backendcore.infrastructure.metrics.TenantMetrics;
import com.lingotlow.backendcore.infrastructure.repository.TenantRepository;
import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import com.lingotlow.backendcore.interfaces.api.exception.ResourceAlreadyExistsException;
import com.lingotlow.backendcore.interfaces.api.exception.ResourceNotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantService Unit Tests")
class TenantServiceTest {

    private final String TENANT_KEY = "test-tenant";
    private final UUID TENANT_ID = UUID.randomUUID();
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantServiceMapper entityMapper;
    @Mock
    private AuditLogger auditLogger;
    @Mock
    private TenantMetrics tenantMetrics;
    @Mock
    private MeterRegistry meterRegistry;
    @InjectMocks
    private TenantService tenantService;
    private TenantEntity sampleTenant;
    private TenantRequestDTO sampleRequestDTO;
    private TenantUpdateDTO sampleUpdateDTO;

    @BeforeEach
    void setUp() {
        sampleTenant = createSampleTenant();
        sampleRequestDTO = createSampleRequestDTO();
        sampleUpdateDTO = createSampleUpdateDTO();
    }

    @Test
    @DisplayName("Should create tenant successfully when tenantKey does not exist")
    void createTenant_Success_WhenTenantKeyDoesNotExist() {
        // Given
        when(tenantRepository.existsByTenantKey(TENANT_KEY)).thenReturn(false);
        when(entityMapper.mapToCreateEntity(sampleRequestDTO)).thenReturn(sampleTenant);
        when(tenantRepository.save(sampleTenant)).thenReturn(sampleTenant);
        when(tenantMetrics.startTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));

        // When
        TenantEntity result = tenantService.createTenant(sampleRequestDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTenantKey()).isEqualTo(TENANT_KEY);
        assertThat(result.getName()).isEqualTo("Test Tenant");
        
        verify(tenantRepository).existsByTenantKey(TENANT_KEY);
        verify(entityMapper).mapToCreateEntity(sampleRequestDTO);
        verify(tenantRepository).save(sampleTenant);
        verify(auditLogger).logTenantCreated(eq(TENANT_KEY), eq("system"), any(Map.class));
        verify(tenantMetrics).incrementTenantCreate();
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when tenantKey already exists")
    void createTenant_ThrowsException_WhenTenantKeyExists() {
        // Given
        when(tenantRepository.existsByTenantKey(TENANT_KEY)).thenReturn(true);
        when(tenantMetrics.startTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));

        // When & Then
        assertThatThrownBy(() -> tenantService.createTenant(sampleRequestDTO))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Tenant")
                .hasMessageContaining(TENANT_KEY);
        
        verify(tenantRepository).existsByTenantKey(TENANT_KEY);
        verify(tenantRepository, never()).save(any());
        verify(auditLogger, never()).logTenantCreated(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should return paginated list of tenants")
    void listTenants_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        List<TenantEntity> tenants = List.of(sampleTenant);
        Page<TenantEntity> expectedPage = new PageImpl<>(tenants, pageable, 1);
        
        when(tenantRepository.findAll(pageable)).thenReturn(expectedPage);
        when(tenantMetrics.startTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));

        // When
        Page<TenantEntity> result = tenantService.listTenants(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTenantKey()).isEqualTo(TENANT_KEY);
        
        verify(tenantRepository).findAll(pageable);
        verify(auditLogger).logTenantListed(eq("system"), eq(0), eq(20), eq(1L));
        verify(tenantMetrics).incrementTenantList();
    }

    @Test
    @DisplayName("Should return tenant when tenantKey exists")
    void getTenantByTenantKey_Success_WhenTenantExists() {
        // Given
        when(tenantRepository.findByTenantKey(TENANT_KEY)).thenReturn(Optional.of(sampleTenant));
        when(tenantMetrics.startTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));

        // When
        TenantEntity result = tenantService.getTenantByTenantKey(TENANT_KEY);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTenantKey()).isEqualTo(TENANT_KEY);
        assertThat(result.getId()).isEqualTo(TENANT_ID);
        
        verify(tenantRepository).findByTenantKey(TENANT_KEY);
        verify(auditLogger).logTenantAccessed(TENANT_KEY, "system");
        verify(tenantMetrics).incrementTenantRead();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when tenantKey does not exist")
    void getTenantByTenantKey_ThrowsException_WhenTenantDoesNotExist() {
        // Given
        when(tenantRepository.findByTenantKey(TENANT_KEY)).thenReturn(Optional.empty());
        when(tenantMetrics.startTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));

        // When & Then
        assertThatThrownBy(() -> tenantService.getTenantByTenantKey(TENANT_KEY))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tenant")
                .hasMessageContaining(TENANT_KEY);
        
        verify(tenantRepository).findByTenantKey(TENANT_KEY);
        verify(auditLogger, never()).logTenantAccessed(anyString(), anyString());
    }

    @Test
    @DisplayName("Should update tenant successfully when tenant exists")
    void updateTenant_Success_WhenTenantExists() {
        // Given
        TenantEntity updatedTenant = createUpdatedTenant();
        when(tenantRepository.findByTenantKey(TENANT_KEY)).thenReturn(Optional.of(sampleTenant));
        when(tenantRepository.save(any(TenantEntity.class))).thenReturn(updatedTenant);
        when(tenantMetrics.startTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));

        // When
        TenantEntity result = tenantService.updateTenant(TENANT_KEY, sampleUpdateDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Tenant");
        assertThat(result.getConfig()).isEqualTo("updated-config");
        
        verify(tenantRepository).findByTenantKey(TENANT_KEY);
        verify(entityMapper).mapToUpdateEntity(sampleUpdateDTO, sampleTenant);
        verify(tenantRepository).save(sampleTenant);
        verify(auditLogger).logTenantUpdated(eq(TENANT_KEY), eq("system"), any(Map.class));
        verify(tenantMetrics).incrementTenantUpdate();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent tenant")
    void updateTenant_ThrowsException_WhenTenantDoesNotExist() {
        // Given
        when(tenantRepository.findByTenantKey(TENANT_KEY)).thenReturn(Optional.empty());
        when(tenantMetrics.startTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));

        // When & Then
        assertThatThrownBy(() -> tenantService.updateTenant(TENANT_KEY, sampleUpdateDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tenant")
                .hasMessageContaining(TENANT_KEY);
        
        verify(tenantRepository).findByTenantKey(TENANT_KEY);
        verify(entityMapper, never()).mapToUpdateEntity(any(), any());
        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete tenant successfully when tenant exists")
    void deleteTenant_Success_WhenTenantExists() {
        // Given
        when(tenantRepository.findByTenantKey(TENANT_KEY)).thenReturn(Optional.of(sampleTenant));
        doNothing().when(tenantRepository).delete(sampleTenant);
        when(tenantMetrics.startTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));

        // When
        tenantService.deleteTenant(TENANT_KEY);

        // Then
        verify(tenantRepository).findByTenantKey(TENANT_KEY);
        verify(tenantRepository).delete(sampleTenant);
        verify(auditLogger).logTenantDeleted(TENANT_KEY, "system");
        verify(tenantMetrics).incrementTenantDelete();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent tenant")
    void deleteTenant_ThrowsException_WhenTenantDoesNotExist() {
        // Given
        when(tenantRepository.findByTenantKey(TENANT_KEY)).thenReturn(Optional.empty());
        when(tenantMetrics.startTimer()).thenReturn(mock(io.micrometer.core.instrument.Timer.Sample.class));

        // When & Then
        assertThatThrownBy(() -> tenantService.deleteTenant(TENANT_KEY))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tenant")
                .hasMessageContaining(TENANT_KEY);
        
        verify(tenantRepository).findByTenantKey(TENANT_KEY);
        verify(tenantRepository, never()).delete(any());
        verify(auditLogger, never()).logTenantDeleted(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return true when tenant exists")
    void tenantExists_ReturnsTrue_WhenTenantExists() {
        // Given
        when(tenantRepository.existsByTenantKey(TENANT_KEY)).thenReturn(true);

        // When
        boolean result = tenantService.tenantExists(TENANT_KEY);

        // Then
        assertThat(result).isTrue();
        verify(tenantRepository).existsByTenantKey(TENANT_KEY);
    }

    @Test
    @DisplayName("Should return false when tenant does not exist")
    void tenantExists_ReturnsFalse_WhenTenantDoesNotExist() {
        // Given
        when(tenantRepository.existsByTenantKey(TENANT_KEY)).thenReturn(false);

        // When
        boolean result = tenantService.tenantExists(TENANT_KEY);

        // Then
        assertThat(result).isFalse();
        verify(tenantRepository).existsByTenantKey(TENANT_KEY);
    }

    // Helper methods
    private TenantEntity createSampleTenant() {
        TenantEntity tenant = new TenantEntity();
        ReflectionTestUtils.setField(tenant, "id", TENANT_ID);
        tenant.setTenantKey(TENANT_KEY);
        tenant.setName("Test Tenant");
        tenant.setConfig("{\"test\": true}");
        tenant.setCreatedAt(OffsetDateTime.now());
        tenant.setUpdatedAt(OffsetDateTime.now());
        return tenant;
    }

    private TenantRequestDTO createSampleRequestDTO() {
        TenantRequestDTO dto = new TenantRequestDTO();
        dto.setTenantKey(TENANT_KEY);
        dto.setName("Test Tenant");
        dto.setConfig("{\"test\": true}");
        return dto;
    }

    private TenantUpdateDTO createSampleUpdateDTO() {
        TenantUpdateDTO dto = new TenantUpdateDTO();
        dto.setName("Updated Tenant");
        dto.setConfig("updated-config");
        return dto;
    }

    private TenantEntity createUpdatedTenant() {
        TenantEntity tenant = createSampleTenant();
        tenant.setName("Updated Tenant");
        tenant.setConfig("updated-config");
        tenant.setUpdatedAt(OffsetDateTime.now());
        return tenant;
    }
}
