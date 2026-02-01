package com.lingotlow.backendcore.interfaces.api.event.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class EventIngestRequest {

    @NotBlank(message = "documentId is required")
    private String documentId;

    @NotBlank(message = "type is required")
    private String type;

    @NotNull(message = "timestamp is required")
    private Long timestamp;

    private Map<String, Object> metadata;
    private Map<String, String> headers;
    private String payload;
}
