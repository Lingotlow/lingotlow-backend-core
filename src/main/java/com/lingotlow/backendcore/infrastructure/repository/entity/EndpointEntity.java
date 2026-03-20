package com.lingotlow.backendcore.infrastructure.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "endpoints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "secret_key", length = 255)
    private String secretKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EndpointStatus status;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum EndpointStatus {
        ACTIVE,
        INACTIVE,
        ERROR
    }

    public EndpointEntity(UUID tenantId, String name, String url, String description, String secretKey, Boolean active, Integer retryCount, Integer timeoutMs) {
        this.tenantId = tenantId;
        this.name = name;
        this.url = url;
        this.description = description;
        this.secretKey = secretKey;
        this.active = active != null ? active : true;
        this.retryCount = retryCount != null ? retryCount : 3;
        this.timeoutMs = timeoutMs != null ? timeoutMs : 30000;
        this.status = EndpointStatus.ACTIVE;
    }
}
