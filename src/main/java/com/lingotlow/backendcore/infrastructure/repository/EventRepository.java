package com.lingotlow.backendcore.infrastructure.repository;

import com.lingotlow.backendcore.infrastructure.repository.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    Optional<EventEntity> findByTenantIdAndDocumentId(UUID tenantId, String documentId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EventEntity e WHERE e.tenantId = :tenantId AND (e.documentId = :documentId OR (e.documentId IS NULL AND :documentId IS NULL))")
    boolean existsByTenantIdAndDocumentId(@Param("tenantId") UUID tenantId, @Param("documentId") String documentId);

    Optional<EventEntity> findByRequestId(UUID requestId);

    @Query("SELECT DISTINCT e.tenantId FROM EventEntity e WHERE e.createdAt >= :startDate AND e.createdAt < :endDate")
    List<UUID> findActiveTenantsByDate(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    @Query("SELECT COUNT(e) FROM EventEntity e WHERE e.tenantId = :tenantId AND e.createdAt >= :startDate AND e.createdAt < :endDate")
    Long countByTenantIdAndCreatedAtBetween(@Param("tenantId") UUID tenantId, @Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    @Query("SELECT COUNT(e) FROM EventEntity e WHERE e.tenantId = :tenantId AND e.status = :status AND e.createdAt >= :startDate AND e.createdAt < :endDate")
    Long countByTenantIdAndStatusAndCreatedAtBetween(@Param("tenantId") UUID tenantId, @Param("status") EventEntity.EventStatus status, @Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    @Query("SELECT COUNT(e) FROM EventEntity e WHERE e.tenantId = :tenantId AND e.retryCount > :retryCount AND e.createdAt >= :startDate AND e.createdAt < :endDate")
    Long countByTenantIdAndRetryCountGreaterThanAndCreatedAtBetween(@Param("tenantId") UUID tenantId, @Param("retryCount") Integer retryCount, @Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    @Query("SELECT e FROM EventEntity e WHERE e.tenantId = :tenantId AND e.status IN :statuses ORDER BY e.createdAt DESC")
    List<EventEntity> findByTenantIdAndStatusInOrderByCreatedAtDesc(@Param("tenantId") UUID tenantId, @Param("statuses") List<EventEntity.EventStatus> statuses);

    @Query("SELECT COUNT(e) FROM EventEntity e WHERE e.status = :status")
    Long countByStatus(@Param("status") EventEntity.EventStatus status);
}
