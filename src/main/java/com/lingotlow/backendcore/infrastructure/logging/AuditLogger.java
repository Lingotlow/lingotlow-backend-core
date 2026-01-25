package com.lingotlow.backendcore.infrastructure.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogger {

  private final ObjectMapper objectMapper;

  public void logTenantOperation(
      String operation, String tenantKey, String userId, Object details) {
    try {
      Map<String, Object> auditEvent =
          Map.of(
              "timestamp",
              OffsetDateTime.now(),
              "eventType",
              "TENANT_OPERATION",
              "operation",
              operation,
              "tenantKey",
              tenantKey,
              "userId",
              userId,
              "details",
              details != null ? details : Map.of(),
              "service",
              "tenant-service");

      log.info("AUDIT: {}", objectMapper.writeValueAsString(auditEvent));
    } catch (Exception e) {
      log.error("Failed to log audit event", e);
    }
  }

  public void logTenantCreated(String tenantKey, String userId, Object tenantData) {
    logTenantOperation("CREATE", tenantKey, userId, tenantData);
  }

  public void logTenantUpdated(String tenantKey, String userId, Object updateData) {
    logTenantOperation("UPDATE", tenantKey, userId, updateData);
  }

  public void logTenantDeleted(String tenantKey, String userId) {
    logTenantOperation("DELETE", tenantKey, userId, null);
  }

  public void logTenantAccessed(String tenantKey, String userId) {
    logTenantOperation("READ", tenantKey, userId, null);
  }

  public void logTenantListed(String userId, int page, int size, long totalElements) {
    try {
      Map<String, Object> auditEvent =
          Map.of(
              "timestamp",
              OffsetDateTime.now(),
              "eventType",
              "TENANT_LISTED",
              "operation",
              "LIST",
              "userId",
              userId,
              "pagination",
              Map.of(
                  "page", page,
                  "size", size,
                  "totalElements", totalElements),
              "service",
              "tenant-service");

      log.info("AUDIT: {}", objectMapper.writeValueAsString(auditEvent));
    } catch (Exception e) {
      log.error("Failed to log audit event", e);
    }
  }
}
