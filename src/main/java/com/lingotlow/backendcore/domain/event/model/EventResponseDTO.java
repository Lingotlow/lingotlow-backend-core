package com.lingotlow.backendcore.domain.event.model;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class EventResponseDTO {

    private UUID requestId;
    private String status;
    private OffsetDateTime timestamp;
    private String message;
}
