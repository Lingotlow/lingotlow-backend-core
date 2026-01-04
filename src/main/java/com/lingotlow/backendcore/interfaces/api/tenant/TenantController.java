package com.lingotlow.backendcore.interfaces.api.tenant;

import com.lingotlow.backendcore.interfaces.api.tenant.model.CreateTenantRequest;
import com.lingotlow.backendcore.interfaces.api.tenant.model.CreateTenantResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

  private final TenantControllerAPI api;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CreateTenantResponse> createTenant(
      @Valid @RequestBody CreateTenantRequest request) {

    return ResponseEntity.status(HttpStatus.CREATED).body(api.createTenant(request));
  }
}
