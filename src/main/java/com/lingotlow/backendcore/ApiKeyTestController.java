package com.lingotlow.backendcore;

import com.lingotlow.backendcore.domain.apikey.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApiKeyTestController {

  private final ApiKeyService apiKeyService;

  @PostMapping("/apikey/{tenantKey}")
  public String generateApiKey(@PathVariable String tenantKey) {
    return apiKeyService.generateApiKey(tenantKey);
  }
}
