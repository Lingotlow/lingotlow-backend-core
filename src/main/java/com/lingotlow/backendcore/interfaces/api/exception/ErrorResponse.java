package com.lingotlow.backendcore.interfaces.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response format")
public class ErrorResponse {

  @Schema(description = "Timestamp when the error occurred", example = "2026-01-25T17:15:00Z")
  private OffsetDateTime timestamp;

  @Schema(description = "HTTP status code", example = "404")
  private int status;

  @Schema(description = "Error type", example = "Resource Not Found")
  private String error;

  @Schema(description = "Detailed error message", example = "Tenant with key 'acme-corp' not found")
  private String message;

  @Schema(description = "Request path that caused the error", example = "/api/tenants/acme-corp")
  private String path;

  @Schema(description = "Validation errors for bad requests")
  private Map<String, String> validationErrors;
}
