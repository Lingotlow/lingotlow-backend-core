package com.lingotlow.backendcore.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestRequest {

  @Valid
  @NotNull(message = "docAttributes is required")
  private DocAttributes docAttributes;

  private Map<String, Object> payload;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DocAttributes {
    @NotBlank(message = "documentId is required")
    private String documentId;

    @NotBlank(message = "type is required")
    private String type;

    @NotBlank(message = "timestamp is required")
    private String timestamp;

    private String source;

    private Map<String, Object> metadata;
  }
}
