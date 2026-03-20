package com.lingotlow.backendcore.infrastructure.repository.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_events_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_events_tenant_document", columnList = "tenant_id, document_id")
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

    @Column(name = "status", nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "received_at")
    private OffsetDateTime receivedAt;

    @Column(name = "s3_key")
    private String s3Key;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;

    @Column(name = "source_ip")
    private String sourceIp;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "last_attempt_at")
    private OffsetDateTime lastAttemptAt;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "response_body")
    private String responseBody;
}
