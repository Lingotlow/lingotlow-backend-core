package com.lingotlow.backendcore.interfaces.api.tenant;

import com.lingotlow.backendcore.domain.tenant.TenantService;
import com.lingotlow.backendcore.interfaces.api.tenant.mapper.TenantCreateRequestToDtoMapper;
import com.lingotlow.backendcore.interfaces.api.tenant.model.TenantCreateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

  private final TenantService tenantService;
  private final TenantCreateRequestToDtoMapper requestMapper;

  public TenantController(
      TenantService tenantService, TenantCreateRequestToDtoMapper requestMapper) {
    this.tenantService = tenantService;
    this.requestMapper = requestMapper;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> createTenant(@RequestBody @Valid TenantCreateRequest request) {
    try {
      tenantService.createTenant(requestMapper.mapToDomain(request));
      return ResponseEntity.status(HttpStatus.CREATED).build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
  }
}
