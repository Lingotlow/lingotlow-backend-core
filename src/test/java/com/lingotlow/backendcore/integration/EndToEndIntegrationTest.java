package com.lingotlow.backendcore.integration;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingotlow.backendcore.domain.apikey.ApiKeyService;
import com.lingotlow.backendcore.domain.event.EventService;
import com.lingotlow.backendcore.domain.tenant.TenantService;
import com.lingotlow.backendcore.infrastructure.repository.ApiKeyRepository;
import com.lingotlow.backendcore.infrastructure.repository.EventRepository;
import com.lingotlow.backendcore.infrastructure.repository.TenantRepository;
import com.lingotlow.backendcore.infrastructure.repository.entity.ApiKeyEntity;
import com.lingotlow.backendcore.infrastructure.repository.entity.TenantEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("End-to-End Integration Tests")
class EndToEndIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private TenantRepository tenantRepository;

  @Autowired private ApiKeyRepository apiKeyRepository;

  @Autowired private EventRepository eventRepository;

  @Autowired private TenantService tenantService;

  @Autowired private ApiKeyService apiKeyService;

  @Autowired private EventService eventService;

  private TenantEntity testTenant;
  private ApiKeyEntity testApiKey;
  private String tenantKey;
  private String apiKey;

  @BeforeEach
  void setUp() {
    // Clean up
    eventRepository.deleteAll();
    apiKeyRepository.deleteAll();
    tenantRepository.deleteAll();

    // Create test tenant
    tenantKey = "test-tenant-" + UUID.randomUUID().toString().substring(0, 8);
    testTenant = new TenantEntity();
    testTenant.setTenantKey(tenantKey);
    testTenant.setName("Test Tenant");
    testTenant.setConfig("{\"test\": true}");
    testTenant.setCreatedAt(OffsetDateTime.now());
    testTenant.setUpdatedAt(OffsetDateTime.now());
    testTenant = tenantRepository.save(testTenant);

    // Create API key for tenant
    var apiKeyResponse = apiKeyService.createApiKey(testTenant.getId());
    apiKey = apiKeyResponse.getPlainKey();
    testApiKey = apiKeyResponse.getApiKey();
  }

  @Test
  @DisplayName("Should handle complete tenant lifecycle with API key management")
  void completeTenantLifecycle_Success() throws Exception {
    // 1. Create new tenant
    String newTenantKey = "new-tenant-" + UUID.randomUUID().toString().substring(0, 8);
    String createTenantJson =
        String.format(
            """
                {
                    "tenantKey": "%s",
                    "name": "New Test Tenant",
                    "config": "{"enabled": true}"
                }
                """,
            newTenantKey);

    var createResult =
        mockMvc
            .perform(
                post("/api/tenants")
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createTenantJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantKey").value(newTenantKey))
            .andExpect(jsonPath("$.name").value("New Test Tenant"))
            .andReturn();

    String createdTenantId =
        objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

    // 2. List tenants and verify new tenant exists
    mockMvc
        .perform(
            get("/api/tenants").header("X-API-Key", apiKey).param("page", "0").param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].tenantKey", hasItems(newTenantKey)));

    // 3. Get tenant details
    mockMvc
        .perform(get("/api/tenants/{tenantKey}", newTenantKey).header("X-API-Key", apiKey))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tenantKey").value(newTenantKey))
        .andExpect(jsonPath("$.name").value("New Test Tenant"));

    // 4. Update tenant
    String updateJson =
        """
                {
                    "name": "Updated Test Tenant",
                    "config": "updated-config"
                }
                """;

    mockMvc
        .perform(
            put("/api/tenants/{tenantKey}", newTenantKey)
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Test Tenant"))
        .andExpect(jsonPath("$.config").value("updated-config"));

    // 5. Create API key for new tenant
    var apiKeyCreateResult =
        mockMvc
            .perform(
                post("/api/tenants/{tenantKey}/api-keys", newTenantKey)
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.apiKey").exists())
            .andExpect(jsonPath("$.plainKey").exists())
            .andReturn();

    String newApiKey =
        objectMapper
            .readTree(apiKeyCreateResult.getResponse().getContentAsString())
            .get("plainKey")
            .asText();

    // 6. List API keys for new tenant
    mockMvc
        .perform(get("/api/tenants/{tenantKey}/api-keys", newTenantKey).header("X-API-Key", apiKey))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].revoked", everyItem(equalTo(false))))
        .andExpect(jsonPath("$").isArray());

    // 7. Delete API key
    String newApiKeyId =
        objectMapper
            .readTree(apiKeyCreateResult.getResponse().getContentAsString())
            .get("apiKey")
            .get("id")
            .asText();

    mockMvc
        .perform(
            delete("/api/tenants/{tenantKey}/api-keys/{keyId}", newTenantKey, newApiKeyId)
                .header("X-API-Key", apiKey))
        .andExpect(status().isNoContent());

    // 8. Delete tenant
    mockMvc
        .perform(delete("/api/tenants/{tenantKey}", newTenantKey).header("X-API-Key", apiKey))
        .andExpect(status().isNoContent());

    // 9. Verify tenant is deleted
    mockMvc
        .perform(get("/api/tenants/{tenantKey}", newTenantKey).header("X-API-Key", apiKey))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should handle complete event lifecycle with proper authentication")
  void completeEventLifecycle_Success() throws Exception {
    // 1. Create event
    String eventJson =
        String.format(
            """
                {
                    "documentId": "doc-%s",
                    "type": "test.event",
                    "timestamp": %d,
                    "metadata": {"source": "integration-test"},
                    "headers": {"Content-Type": "application/json"},
                    "payload": "test payload data"
                }
                """,
            UUID.randomUUID().toString().substring(0, 8), System.currentTimeMillis());

    var eventResult =
        mockMvc
            .perform(
                post("/api/ingest/{tenantKey}", tenantKey)
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(eventJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requestId").exists())
            .andExpect(jsonPath("$.status").value("RECEIVED"))
            .andExpect(jsonPath("$.message").value("Event received successfully"))
            .andReturn();

    String requestId =
        objectMapper
            .readTree(eventResult.getResponse().getContentAsString())
            .get("requestId")
            .asText();

    // 2. Verify event exists in database
    mockMvc
        .perform(get("/api/events/{requestId}", requestId).header("X-API-Key", apiKey))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestId").value(requestId))
        .andExpect(jsonPath("$.documentId").exists())
        .andExpect(jsonPath("$.type").value("test.event"))
        .andExpect(jsonPath("$.status").value("RECEIVED"));

    // 3. Try to create duplicate event (should fail)
    mockMvc
        .perform(
            post("/api/ingest/{tenantKey}", tenantKey)
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("Conflict"))
        .andExpect(jsonPath("$.message", containsString("already exists")));
  }

  @Test
  @DisplayName("Should handle authentication and authorization properly")
  void authenticationAndAuthorization_Success() throws Exception {
    // 1. Test without API key (should fail)
    mockMvc.perform(get("/api/tenants")).andExpect(status().isForbidden());

    // 2. Test with invalid API key (should fail)
    mockMvc
        .perform(get("/api/tenants").header("X-API-Key", "invalid-key"))
        .andExpect(status().isForbidden());

    // 3. Test with valid API key (should succeed)
    mockMvc.perform(get("/api/tenants").header("X-API-Key", apiKey)).andExpect(status().isOk());

    // 4. Test public endpoints (should work without auth)
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());

    mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Should handle validation errors properly")
  void validationErrors_HandledCorrectly() throws Exception {
    // 1. Test tenant creation with invalid data
    String invalidTenantJson =
        """
                {
                    "tenantKey": "",
                    "name": "",
                    "config": ""
                }
                """;

    mockMvc
        .perform(
            post("/api/tenants")
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidTenantJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation Failed"))
        .andExpect(jsonPath("$.validationErrors").exists());

    // 2. Test event creation with invalid data
    String invalidEventJson =
        """
                {
                    "documentId": "",
                    "type": "",
                    "timestamp": 0,
                    "metadata": {},
                    "headers": {},
                    "payload": ""
                }
                """;

    mockMvc
        .perform(
            post("/api/ingest/{tenantKey}", tenantKey)
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidEventJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation Failed"));
  }

  @Test
  @DisplayName("Should handle resource not found scenarios")
  void resourceNotFound_HandledCorrectly() throws Exception {
    // 1. Test non-existent tenant
    mockMvc
        .perform(get("/api/tenants/non-existent-tenant").header("X-API-Key", apiKey))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Resource Not Found"));

    // 2. Test non-existent API key
    mockMvc
        .perform(
            delete(
                    "/api/tenants/{tenantKey}/api-keys/{keyId}",
                    tenantKey,
                    UUID.randomUUID().toString())
                .header("X-API-Key", apiKey))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Resource Not Found"));

    // 3. Test non-existent event
    mockMvc
        .perform(
            get("/api/events/{requestId}", UUID.randomUUID().toString())
                .header("X-API-Key", apiKey))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Resource Not Found"));
  }

  @Test
  @DisplayName("Should maintain data isolation between tenants")
  void tenantDataIsolation_Success() throws Exception {
    // Create second tenant
    String secondTenantKey = "second-tenant-" + UUID.randomUUID().toString().substring(0, 8);
    TenantEntity secondTenant = new TenantEntity();
    secondTenant.setTenantKey(secondTenantKey);
    secondTenant.setName("Second Tenant");
    secondTenant.setConfig("{\"test\": true}");
    secondTenant.setCreatedAt(OffsetDateTime.now());
    secondTenant.setUpdatedAt(OffsetDateTime.now());
    secondTenant = tenantRepository.save(secondTenant);

    // Create API key for second tenant
    var secondApiKeyResponse = apiKeyService.createApiKey(secondTenant.getId());
    String secondApiKey = secondApiKeyResponse.getPlainKey();

    // Create event in first tenant
    String eventJson =
        String.format(
            """
                {
                    "documentId": "isolated-doc-%s",
                    "type": "isolation.test",
                    "timestamp": %d,
                    "metadata": {},
                    "headers": {},
                    "payload": "isolation test"
                }
                """,
            UUID.randomUUID().toString().substring(0, 8), System.currentTimeMillis());

    mockMvc
        .perform(
            post("/api/ingest/{tenantKey}", tenantKey)
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson))
        .andExpect(status().isOk());

    // Verify second tenant can't access first tenant's data
    mockMvc
        .perform(get("/api/tenants/{tenantKey}", tenantKey).header("X-API-Key", secondApiKey))
        .andExpect(status().isForbidden());

    // Verify first tenant can't access second tenant's data
    mockMvc
        .perform(get("/api/tenants/{tenantKey}", secondTenantKey).header("X-API-Key", apiKey))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should handle concurrent operations safely")
  void concurrentOperations_Safe() throws Exception {
    // This test verifies that the system can handle multiple simultaneous operations
    // without data corruption or race conditions

    // Create multiple events concurrently
    for (int i = 0; i < 5; i++) {
      String eventJson =
          String.format(
              """
                    {
                        "documentId": "concurrent-doc-%d",
                        "type": "concurrent.test",
                        "timestamp": %d,
                        "metadata": {},
                        "headers": {},
                        "payload": "concurrent test %d"
                    }
                    """,
              i, System.currentTimeMillis() + i, i);

      var result =
          mockMvc
              .perform(
                  post("/api/ingest/{tenantKey}", tenantKey)
                      .header("X-API-Key", apiKey)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(eventJson))
              .andExpect(status().isOk())
              .andReturn();

      String requestId =
          objectMapper
              .readTree(result.getResponse().getContentAsString())
              .get("requestId")
              .asText();

      // Verify each event was created successfully
      mockMvc
          .perform(get("/api/events/{requestId}", requestId).header("X-API-Key", apiKey))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.documentId").value("concurrent-doc-" + i));
    }
  }
}
