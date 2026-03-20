package com.lingotlow.backendcore.infrastructure.repository;

import com.lingotlow.backendcore.infrastructure.repository.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    Optional<EventEntity> findByTenantIdAndDocumentId(UUID tenantId, String documentId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EventEntity e WHERE e.tenantId = :tenantId AND (e.documentId = :documentId OR (e.documentId IS NULL AND :documentId IS NULL))")
    boolean existsByTenantIdAndDocumentId(@Param("tenantId") UUID tenantId, @Param("documentId") String documentId);

    Optional<EventEntity> findByRequestId(UUID requestId);
}
