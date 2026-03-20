package com.lingotlow.backendcore.infrastructure.repository.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

@Data
@Entity
@Table(name = "tenants", indexes = @Index(name = "idx_tenants_key", columnList = "tenant_key"))
public class TenantEntity {

  @Id 
  @UuidGenerator 
  private UUID id;

  @Column(name = "tenant_key", unique = true, nullable = false)
  private String tenantKey;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String config;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
