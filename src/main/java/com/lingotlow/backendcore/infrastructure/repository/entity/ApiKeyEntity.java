package com.lingotlow.backendcore.infrastructure.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "api_keys")
public class ApiKeyEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", columnDefinition = "uuid", nullable = false)
    private UUID tenantId;

    @Column(name = "key_hash", nullable = false)
    private String keyHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

}
