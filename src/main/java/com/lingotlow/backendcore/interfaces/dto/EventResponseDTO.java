package com.lingotlow.backendcore.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDTO {
    private UUID id;
    private UUID requestId;
    private String documentId;
    private String type;
    private String status;
    private Integer retryCount;
    private String payload;
    private String headers;
    private String metadata;
    private String failureReason;
    private Instant receivedAt;
    private Instant deliveredAt;
    private Instant createdAt;
}
