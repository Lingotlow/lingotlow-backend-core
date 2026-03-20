package com.lingotlow.backendcore.infrastructure.repository;

import com.lingotlow.backendcore.infrastructure.repository.entity.MetricsDailyAggregateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetricsDailyAggregateRepository extends JpaRepository<MetricsDailyAggregateEntity, Long> {

    Optional<MetricsDailyAggregateEntity> findByTenantIdAndAggregateDate(UUID tenantId, LocalDate date);

    List<MetricsDailyAggregateEntity> findByTenantIdAndAggregateDateBetweenOrderByAggregateDateDesc(
            UUID tenantId, LocalDate startDate, LocalDate endDate);

    List<MetricsDailyAggregateEntity> findByAggregateDateBetweenOrderByAggregateDateDesc(
            LocalDate startDate, LocalDate endDate);

    @Query("SELECT m FROM MetricsDailyAggregateEntity m WHERE m.tenantId = :tenantId ORDER BY m.aggregateDate DESC")
    List<MetricsDailyAggregateEntity> findLatestByTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT m FROM MetricsDailyAggregateEntity m WHERE m.aggregateDate = :date ORDER BY m.eventsReceived DESC")
    List<MetricsDailyAggregateEntity> findTopByDateOrderByEventsReceived(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(m.eventsReceived), 0) FROM MetricsDailyAggregateEntity m WHERE m.aggregateDate = :date")
    Long totalEventsReceivedByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(m.eventsDelivered), 0) FROM MetricsDailyAggregateEntity m WHERE m.aggregateDate = :date")
    Long totalEventsDeliveredByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(m.eventsFailed), 0) FROM MetricsDailyAggregateEntity m WHERE m.aggregateDate = :date")
    Long totalEventsFailedByDate(@Param("date") LocalDate date);
}
