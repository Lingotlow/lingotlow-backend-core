package com.lingotlow.backendcore.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointRequest {

  @NotBlank(message = "Name is required")
  @Size(max = 100, message = "Name must be less than 100 characters")
  private String name;

  @NotBlank(message = "URL is required")
  @Size(max = 500, message = "URL must be less than 500 characters")
  private String url;

  @NotNull(message = "Retry count is required")
  @Positive(message = "Retry count must be positive")
  @Builder.Default
  private Integer retryCount = 3;

  @NotNull(message = "Timeout is required")
  @Positive(message = "Timeout must be positive")
  @Builder.Default
  private Integer timeoutMs = 5000;

  @Size(max = 255, message = "Secret must be less than 255 characters")
  private String secret;

  @Builder.Default private Boolean active = true;

  @Size(max = 500, message = "Description must be less than 500 characters")
  private String description;
}
