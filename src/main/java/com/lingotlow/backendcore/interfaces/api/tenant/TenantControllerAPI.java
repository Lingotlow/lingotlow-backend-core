package com.lingotlow.backendcore.interfaces.api.tenant;

import com.lingotlow.backendcore.application.tenant.dto.TenantCreateCommand;
import com.lingotlow.backendcore.application.tenant.dto.TenantResult;
import com.lingotlow.backendcore.application.tenant.usecase.CreateTenantUseCase;
import com.lingotlow.backendcore.interfaces.api.tenant.model.CreateTenantRequest;
import com.lingotlow.backendcore.interfaces.api.tenant.model.CreateTenantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantControllerAPI {

  // TODO: Adicionar googlejavaformat no readme.md

  private final CreateTenantUseCase createTenantUseCase;

  public CreateTenantResponse createTenant(CreateTenantRequest request) {

    TenantCreateCommand command =
        new TenantCreateCommand(request.tenantKey(), request.name(), request.config());

    TenantResult result = createTenantUseCase.execute(command);

    return new CreateTenantResponse(
        result.id(), result.tenantKey(), result.name(), result.config(), result.createdAt());
  }
}
