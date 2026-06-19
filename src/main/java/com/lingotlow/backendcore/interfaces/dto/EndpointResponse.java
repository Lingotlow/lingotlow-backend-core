package com.lingotlow.backendcore.interfaces.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointResponse {

    private UUID id;
    private String name;
    private String url;
    private Integer retryCount;
    private Integer timeoutMs;
    private Boolean active;
    private String status;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}