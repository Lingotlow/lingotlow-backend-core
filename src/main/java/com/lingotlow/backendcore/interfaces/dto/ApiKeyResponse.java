package com.lingotlow.backendcore.interfaces.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {
    private UUID id;
    private String prefix;
    private Instant createdAt;
    private String key;

    // Constructor for creation response (full key)
    public ApiKeyResponse(String key) {
        this.key = key;
    }

    // Constructor for list response (without full key)
    public ApiKeyResponse(UUID id, String prefix, Instant createdAt) {
        this.id = id;
        this.prefix = prefix;
        this.createdAt = createdAt;
    }
}