package com.lingotlow.backendcore.infrastructure.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "metrics_daily_aggregate", indexes = {
    @Index(name = "idx_metrics_tenant_date", columnList = "tenant_id, aggregate_date"),
    @Index(name = "idx_metrics_date", columnList = "aggregate_date")
})
public class MetricsDailyAggregateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "aggregate_date", nullable = false)
    private LocalDate aggregateDate;

    @Column(name = "events_received", nullable = false)
    @Builder.Default
    private Long eventsReceived = 0L;

    @Column(name = "events_delivered", nullable = false)
    @Builder.Default
    private Long eventsDelivered = 0L;

    @Column(name = "events_failed", nullable = false)
    @Builder.Default
    private Long eventsFailed = 0L;

    @Column(name = "events_retried", nullable = false)
    @Builder.Default
    private Long eventsRetried = 0L;

    @Column(name = "active_endpoints", nullable = false)
    @Builder.Default
    private Integer activeEndpoints = 0;

    @Column(name = "avg_delivery_time_ms")
    private Double avgDeliveryTimeMs;

    @Column(name = "max_delivery_time_ms")
    private Long maxDeliveryTimeMs;

    @Column(name = "min_delivery_time_ms")
    private Long minDeliveryTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private java.time.OffsetDateTime createdAt;
}
