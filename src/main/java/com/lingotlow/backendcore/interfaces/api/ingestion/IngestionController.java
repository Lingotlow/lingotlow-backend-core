package com.lingotlow.backendcore.interfaces.api.ingestion;

import com.lingotlow.backendcore.infrastructure.service.IngestionService;
import com.lingotlow.backendcore.interfaces.dto.IngestRequest;
import com.lingotlow.backendcore.interfaces.dto.IngestResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
@Slf4j
public class IngestionController {

  private final IngestionService ingestionService;

  @PostMapping("/{tenantKey}")
  public ResponseEntity<IngestResponse> ingest(
      @PathVariable String tenantKey,
      @RequestHeader(value = "X-API-Key", required = false) String apiKey,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody IngestRequest request) {

    log.info("Ingest request received for tenant: {}", tenantKey);

    // Validar API Key
    if (apiKey == null || apiKey.isEmpty()) {
      log.warn("Missing API Key for tenant: {}", tenantKey);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(IngestResponse.builder().status("UNAUTHORIZED").receivedAt(Instant.now()).build());
    }

    try {
      UUID requestId = ingestionService.ingestWebhook(tenantKey, apiKey, request, idempotencyKey);

      return ResponseEntity.ok(
          IngestResponse.builder()
              .requestId(requestId)
              .status("RECEIVED")
              .receivedAt(Instant.now())
              .build());

    } catch (IllegalArgumentException e) {
      log.warn("Tenant not found: {}", tenantKey);
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(IngestResponse.builder().status("ERROR").receivedAt(Instant.now()).build());
    } catch (SecurityException e) {
      log.warn("Invalid API Key for tenant: {}", tenantKey);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(IngestResponse.builder().status("UNAUTHORIZED").receivedAt(Instant.now()).build());
    } catch (Exception e) {
      log.error("Error processing ingest request", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(IngestResponse.builder().status("ERROR").receivedAt(Instant.now()).build());
    }
  }
}
