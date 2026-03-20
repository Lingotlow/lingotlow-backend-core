package com.lingotlow.backendcore.infrastructure.repository.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_events_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_events_tenant_document", columnList = "tenant_id, document_id"),
    @Index(name = "idx_events_request_id", columnList = "request_id")
})
public class EventEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", columnDefinition = "uuid", nullable = false)
    private UUID tenantId;

    @Column(name = "request_id", columnDefinition = "uuid", nullable = false, unique = true)
    private UUID requestId;

    @Column(name = "document_id")
    private String documentId;

    @Column(name = "type")
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EventStatus status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "source_ip")
    private String sourceIp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "last_retry_at")
    private OffsetDateTime lastRetryAt;

    public enum EventStatus {
        PENDING,
        DELIVERED,
        FAILED,
        RETRY
    }
}
