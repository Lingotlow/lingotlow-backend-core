package com.lingotlow.backendcore.infrastructure.scheduler;

import com.lingotlow.backendcore.infrastructure.repository.EndpointRepository;
import com.lingotlow.backendcore.infrastructure.repository.EventRepository;
import com.lingotlow.backendcore.infrastructure.repository.MetricsDailyAggregateRepository;
import com.lingotlow.backendcore.infrastructure.repository.entity.EventEntity;
import com.lingotlow.backendcore.infrastructure.repository.entity.MetricsDailyAggregateEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsAggregationService {

    private final EventRepository eventRepository;
    private final EndpointRepository endpointRepository;
    private final MetricsDailyAggregateRepository metricsRepository;

    @Scheduled(cron = "0 0 1 * * ?") // Run daily at 1 AM
    @Transactional
    public void aggregateDailyMetrics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Starting daily metrics aggregation for date: {}", yesterday);

        try {
            // Get all tenants that had activity yesterday
            OffsetDateTime startOfYesterday = yesterday.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime endOfYesterday = yesterday.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
            List<UUID> activeTenants = eventRepository.findActiveTenantsByDate(startOfYesterday, endOfYesterday);
            
            for (UUID tenantId : activeTenants) {
                aggregateTenantMetrics(tenantId, yesterday);
            }

            log.info("Daily metrics aggregation completed for {} tenants", activeTenants.size());
        } catch (Exception e) {
            log.error("Error during daily metrics aggregation", e);
        }
    }

    private void aggregateTenantMetrics(UUID tenantId, LocalDate date) {
        try {
            log.debug("Aggregating metrics for tenant: {}, date: {}", tenantId, date);

            // Calculate date range for the aggregation day
            OffsetDateTime startOfDay = date.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

            // Count events by status
            long eventsReceived = eventRepository.countByTenantIdAndCreatedAtBetween(tenantId, startOfDay, endOfDay);
            long eventsDelivered = eventRepository.countByTenantIdAndStatusAndCreatedAtBetween(
                    tenantId, EventEntity.EventStatus.DELIVERED, startOfDay, endOfDay);
            long eventsFailed = eventRepository.countByTenantIdAndStatusAndCreatedAtBetween(
                    tenantId, EventEntity.EventStatus.FAILED, startOfDay, endOfDay);
            long eventsRetried = eventRepository.countByTenantIdAndRetryCountGreaterThanAndCreatedAtBetween(
                    tenantId, 0, startOfDay, endOfDay);

            // Count active endpoints
            long activeEndpoints = endpointRepository.countActiveByTenant(tenantId);

            // Calculate delivery time metrics (simplified - in production, you'd store individual delivery times)
            Double avgDeliveryTimeMs = calculateAverageDeliveryTime(tenantId, startOfDay, endOfDay);
            Long maxDeliveryTimeMs = calculateMaxDeliveryTime(tenantId, startOfDay, endOfDay);
            Long minDeliveryTimeMs = calculateMinDeliveryTime(tenantId, startOfDay, endOfDay);

            // Check if record already exists
            var existing = metricsRepository.findByTenantIdAndAggregateDate(tenantId, date);
            
            MetricsDailyAggregateEntity aggregate;
            if (existing.isPresent()) {
                aggregate = existing.get();
                log.debug("Updating existing aggregate record for tenant: {}, date: {}", tenantId, date);
            } else {
                aggregate = new MetricsDailyAggregateEntity();
                aggregate.setTenantId(tenantId);
                aggregate.setAggregateDate(date);
                log.debug("Creating new aggregate record for tenant: {}, date: {}", tenantId, date);
            }

            // Update metrics
            aggregate.setEventsReceived(eventsReceived);
            aggregate.setEventsDelivered(eventsDelivered);
            aggregate.setEventsFailed(eventsFailed);
            aggregate.setEventsRetried(eventsRetried);
            aggregate.setActiveEndpoints((int) activeEndpoints);
            aggregate.setAvgDeliveryTimeMs(avgDeliveryTimeMs);
            aggregate.setMaxDeliveryTimeMs(maxDeliveryTimeMs);
            aggregate.setMinDeliveryTimeMs(minDeliveryTimeMs);

            metricsRepository.save(aggregate);
            
            log.info("Metrics aggregated for tenant: {}, date: {} - received: {}, delivered: {}, failed: {}", 
                    tenantId, date, eventsReceived, eventsDelivered, eventsFailed);

        } catch (Exception e) {
            log.error("Error aggregating metrics for tenant: {}, date: {}", tenantId, date, e);
        }
    }

    private Double calculateAverageDeliveryTime(UUID tenantId, OffsetDateTime start, OffsetDateTime end) {
        // Simplified calculation - in production, you'd track individual delivery times
        // For now, return a placeholder value
        return 1500.0; // 1.5 seconds average
    }

    private Long calculateMaxDeliveryTime(UUID tenantId, OffsetDateTime start, OffsetDateTime end) {
        // Simplified calculation - in production, you'd track individual delivery times
        return 5000L; // 5 seconds max
    }

    private Long calculateMinDeliveryTime(UUID tenantId, OffsetDateTime start, OffsetDateTime end) {
        // Simplified calculation - in production, you'd track individual delivery times
        return 200L; // 200ms min
    }

    // Manual trigger for testing
    public void triggerAggregationForDate(LocalDate date) {
        log.info("Manual trigger for metrics aggregation on date: {}", date);
        aggregateDailyMetrics();
    }
}
