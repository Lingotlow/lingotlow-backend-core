package com.lingotlow.backendcore.infrastructure.repository;

import com.lingotlow.backendcore.domain.event.model.Event;
import com.lingotlow.backendcore.domain.tenant.model.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    Optional<Event> findByRequestId(UUID requestId);

    // Método sem filtros
    Page<Event> findByTenant(Tenant tenant, Pageable pageable);

    // Método com filtro de status
    Page<Event> findByTenantAndStatus(Tenant tenant, String status, Pageable pageable);

    // Método com filtro de data
    @Query("SELECT e FROM Event e WHERE e.tenant = :tenant AND e.createdAt >= :fromDate AND e.createdAt <= :toDate")
    Page<Event> findByTenantAndDateRange(@Param("tenant") Tenant tenant,
                                         @Param("fromDate") Instant fromDate,
                                         @Param("toDate") Instant toDate,
                                         Pageable pageable);

    // Método com todos os filtros
    @Query("SELECT e FROM Event e WHERE e.tenant = :tenant AND e.status = :status AND e.createdAt >= :fromDate AND e.createdAt <= :toDate")
    Page<Event> findByTenantAndStatusAndDateRange(@Param("tenant") Tenant tenant,
                                                  @Param("status") String status,
                                                  @Param("fromDate") Instant fromDate,
                                                  @Param("toDate") Instant toDate,
                                                  Pageable pageable);
}
