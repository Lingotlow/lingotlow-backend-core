package com.lingotlow.backendcore.interfaces.api.tenant;

import com.lingotlow.backendcore.domain.tenant.TenantService;
import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import com.lingotlow.backendcore.interfaces.api.tenant.mapper.TenantControllerMapper;
import com.lingotlow.backendcore.interfaces.api.tenant.model.TenantCreateRequest;
import com.lingotlow.backendcore.interfaces.api.tenant.model.TenantResponse;
import com.lingotlow.backendcore.interfaces.api.tenant.model.TenantSummary;
import com.lingotlow.backendcore.interfaces.api.tenant.model.TenantUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
@Tag(name = "Tenant Management", description = "API for managing tenants")
public class TenantController {

  private final TenantService tenantService;
  private final TenantControllerMapper requestMapper;

  public TenantController(TenantService tenantService, TenantControllerMapper requestMapper) {
    this.tenantService = tenantService;
    this.requestMapper = requestMapper;
  }

  @PostMapping
  @Operation(
      summary = "Create new tenant",
      description = "Creates a new tenant with the provided data")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Tenant created successfully"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(
            responseCode = "409",
            description = "Tenant already exists",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public ResponseEntity<TenantResponse> createTenant(
      @RequestBody @Valid TenantCreateRequest request) {
    TenantEntity created = tenantService.createTenant(requestMapper.mapToDomain(request));
    TenantResponse response = requestMapper.mapToResponse(created);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "List tenants", description = "Lists all tenants with pagination")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Tenants listed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  public ResponseEntity<Page<TenantSummary>> listTenants(
      @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<TenantEntity> tenants = tenantService.listTenants(pageable);
    Page<TenantSummary> response = tenants.map(requestMapper::mapToSummary);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{tenantKey}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Get tenant details",
      description = "Retrieves detailed information about a specific tenant")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Tenant found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Tenant not found")
      })
  public ResponseEntity<TenantResponse> getTenant(
      @Parameter(description = "Tenant key") @PathVariable String tenantKey) {

    TenantEntity entity = tenantService.getTenantByTenantKey(tenantKey);
    TenantResponse response = requestMapper.mapToResponse(entity);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{tenantKey}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Update tenant", description = "Updates an existing tenant's information")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Tenant updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Tenant not found")
      })
  public ResponseEntity<TenantResponse> updateTenant(
      @Parameter(description = "Tenant key") @PathVariable String tenantKey,
      @RequestBody @Valid TenantUpdateRequest request) {

    TenantEntity entity = tenantService.updateTenant(tenantKey, requestMapper.mapToDomain(request));
    TenantResponse response = requestMapper.mapToResponse(entity);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{tenantKey}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Delete tenant", description = "Deletes a tenant (optional operation)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Tenant deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Tenant not found")
      })
  public ResponseEntity<Void> deleteTenant(
      @Parameter(description = "Tenant key") @PathVariable String tenantKey) {

    tenantService.deleteTenant(tenantKey);
    return ResponseEntity.noContent().build();
  }
}
