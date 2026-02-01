package com.lingotlow.backendcore.interfaces.api.event.model;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class EventIngestResponse {

    private UUID requestId;
    private String status;
    private OffsetDateTime timestamp;
    private String message;
}
