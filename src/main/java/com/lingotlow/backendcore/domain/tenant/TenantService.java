package com.lingotlow.backendcore.domain.tenant;

import com.lingotlow.backendcore.domain.tenant.mapper.TenantServiceMapper;
import com.lingotlow.backendcore.domain.tenant.model.TenantRequestDTO;
import com.lingotlow.backendcore.domain.tenant.model.TenantUpdateDTO;
import com.lingotlow.backendcore.infrastructure.logging.AuditLogger;
import com.lingotlow.backendcore.infrastructure.metrics.TenantMetrics;
import com.lingotlow.backendcore.infrastructure.repository.TenantRepository;
import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import com.lingotlow.backendcore.interfaces.api.exception.ResourceAlreadyExistsException;
import com.lingotlow.backendcore.interfaces.api.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class TenantService {

  private final TenantRepository tenantRepository;
  private final TenantServiceMapper entityMapper;
  private final AuditLogger auditLogger;
  private final TenantMetrics tenantMetrics;

  public TenantService(
      TenantRepository tenantRepository,
      TenantServiceMapper entityMapper,
      AuditLogger auditLogger,
      TenantMetrics tenantMetrics) {
    this.tenantRepository = tenantRepository;
    this.entityMapper = entityMapper;
    this.auditLogger = auditLogger;
    this.tenantMetrics = tenantMetrics;
  }

  public TenantEntity createTenant(TenantRequestDTO request) {
    var timerSample = tenantMetrics.startTimer();
    try {
      log.info("Creating tenant with key: {}", request.getTenantKey());

      if (tenantRepository.existsByTenantKey(request.getTenantKey())) {
        log.warn("Tenant creation failed - key already exists: {}", request.getTenantKey());
        tenantMetrics.recordTimer(timerSample, "create", "error");
        throw new ResourceAlreadyExistsException("Tenant", request.getTenantKey());
      }

      TenantEntity created = tenantRepository.save(entityMapper.mapToCreateEntity(request));
      log.info(
          "Tenant created successfully with ID: {} and key: {}",
          created.getId(),
          created.getTenantKey());

      auditLogger.logTenantCreated(
          created.getTenantKey(),
          "system",
          Map.of(
              "id", created.getId(),
              "name", created.getName(),
              "createdAt", created.getCreatedAt()));

      tenantMetrics.incrementTenantCreate();
      tenantMetrics.recordTimer(timerSample, "create", "success");
      return created;
    } catch (Exception e) {
      tenantMetrics.recordTimer(timerSample, "create", "error");
      throw e;
    }
  }

  @Transactional(readOnly = true)
  public Page<TenantEntity> listTenants(Pageable pageable) {
    var timerSample = tenantMetrics.startTimer();
    try {
      log.info(
          "Listing tenants with pagination: page={}, size={}",
          pageable.getPageNumber(),
          pageable.getPageSize());

      Page<TenantEntity> result = tenantRepository.findAll(pageable);
      log.info("Found {} tenants total", result.getTotalElements());

      auditLogger.logTenantListed(
          "system", pageable.getPageNumber(), pageable.getPageSize(), result.getTotalElements());

      tenantMetrics.incrementTenantList();
      tenantMetrics.recordTimer(timerSample, "list", "success");
      return result;
    } catch (Exception e) {
      tenantMetrics.recordTimer(timerSample, "list", "error");
      throw e;
    }
  }

  @Transactional(readOnly = true)
  public TenantEntity getTenantByTenantKey(String tenantKey) {
    var timerSample = tenantMetrics.startTimer();
    try {
      log.info("Retrieving tenant with key: {}", tenantKey);

      TenantEntity entity =
          tenantRepository
              .findByTenantKey(tenantKey)
              .orElseThrow(
                  () -> {
                    log.warn("Tenant not found: {}", tenantKey);
                    tenantMetrics.recordTimer(timerSample, "read", "not_found");
                    return new ResourceNotFoundException("Tenant", tenantKey);
                  });

      log.info("Tenant retrieved successfully: {} (ID: {})", entity.getTenantKey(), entity.getId());
      auditLogger.logTenantAccessed(entity.getTenantKey(), "system");

      tenantMetrics.incrementTenantRead();
      tenantMetrics.recordTimer(timerSample, "read", "success");
      return entity;
    } catch (Exception e) {
      if (!(e instanceof ResourceNotFoundException)) {
        tenantMetrics.recordTimer(timerSample, "read", "error");
      }
      throw e;
    }
  }

  public TenantEntity updateTenant(String tenantKey, TenantUpdateDTO updateDTO) {
    var timerSample = tenantMetrics.startTimer();
    try {
      log.info("Updating tenant with key: {}", tenantKey);

      TenantEntity tenant =
          tenantRepository
              .findByTenantKey(tenantKey)
              .orElseThrow(
                  () -> {
                    log.warn("Tenant update failed - not found: {}", tenantKey);
                    tenantMetrics.recordTimer(timerSample, "update", "not_found");
                    return new ResourceNotFoundException("Tenant", tenantKey);
                  });

      entityMapper.mapToUpdateEntity(updateDTO, tenant);
      tenant.setUpdatedAt(OffsetDateTime.now());

      TenantEntity updated = tenantRepository.save(tenant);
      log.info("Tenant updated successfully: {} (ID: {})", updated.getTenantKey(), updated.getId());

      auditLogger.logTenantUpdated(
          updated.getTenantKey(),
          "system",
          Map.of(
              "id", updated.getId(),
              "updatedAt", updated.getUpdatedAt()));

      tenantMetrics.incrementTenantUpdate();
      tenantMetrics.recordTimer(timerSample, "update", "success");
      return updated;
    } catch (Exception e) {
      if (!(e instanceof ResourceNotFoundException)) {
        tenantMetrics.recordTimer(timerSample, "update", "error");
      }
      throw e;
    }
  }

  public void deleteTenant(String tenantKey) {
    var timerSample = tenantMetrics.startTimer();
    try {
      log.info("Deleting tenant with key: {}", tenantKey);

      TenantEntity tenant =
          tenantRepository
              .findByTenantKey(tenantKey)
              .orElseThrow(
                  () -> {
                    log.warn("Tenant deletion failed - not found: {}", tenantKey);
                    tenantMetrics.recordTimer(timerSample, "delete", "not_found");
                    return new ResourceNotFoundException("Tenant", tenantKey);
                  });

      tenantRepository.delete(tenant);
      log.info("Tenant deleted successfully: {} (ID: {})", tenant.getTenantKey(), tenant.getId());

      auditLogger.logTenantDeleted(tenant.getTenantKey(), "system");

      tenantMetrics.incrementTenantDelete();
      tenantMetrics.recordTimer(timerSample, "delete", "success");
    } catch (Exception e) {
      if (!(e instanceof ResourceNotFoundException)) {
        tenantMetrics.recordTimer(timerSample, "delete", "error");
      }
      throw e;
    }
  }

  @Transactional(readOnly = true)
  public boolean tenantExists(String tenantKey) {
    boolean exists = tenantRepository.existsByTenantKey(tenantKey);
    log.debug("Tenant existence check for key {}: {}", tenantKey, exists);
    return exists;
  }
}
