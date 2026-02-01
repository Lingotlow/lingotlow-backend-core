package com.lingotlow.backendcore.infrastructure.queue;

import com.lingotlow.backendcore.infrastructure.repository.EventRepository;
import com.lingotlow.backendcore.infrastructure.repository.entity.EventEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class QueueFailureHandler {

    private final EventRepository eventRepository;
    private final EventQueueProducer eventQueueProducer;

    public QueueFailureHandler(EventRepository eventRepository, EventQueueProducer eventQueueProducer) {
        this.eventRepository = eventRepository;
        this.eventQueueProducer = eventQueueProducer;
    }

    @Async
    @Transactional
    public void handleQueueFailure(UUID requestId, UUID tenantId, String errorMessage) {
        log.warn("Handling queue failure for requestId: {}, tenantId: {}, error: {}",
                requestId, tenantId, errorMessage);

        try {
            // Mark event as queue failed for possible reprocessing
            EventEntity event = eventRepository.findByRequestId(requestId)
                    .orElse(null);

            if (event != null) {
                // Add metadata about queue failure
                // We could have a field in the entity to track queue status
                log.info("Event found for queue failure handling - requestId: {}, documentId: {}",
                        requestId, event.getDocumentId());

                // Here we could implement retry or DLQ logic
                // For now, we just log for monitoring
            }
        } catch (Exception e) {
            log.error("Error handling queue failure for requestId: {}, error: {}",
                    requestId, e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional(readOnly = true)
    public void checkQueueHealth() {
        try {
            boolean isHealthy = eventQueueProducer.isQueueHealthy();

            if (!isHealthy) {
                log.error("Queue health check failed - Redis Streams may be unavailable");
                // Here we could implement alert logic
            } else {
                log.debug("Queue health check passed");
            }
        } catch (Exception e) {
            log.error("Error during queue health check: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 */2 * * *") // Every 2 hours
    @Transactional
    public void reprocessFailedEvents() {
        log.info("Starting reprocessing of failed queue events");

        try {
            // Search for events created in the last 2 hours that may need reprocessing
            LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);

            // This would be an implementation to search for events that failed in the queue
            // For now, we just log that the process was executed
            log.info("Reprocessing check completed for events since: {}", twoHoursAgo);

        } catch (Exception e) {
            log.error("Error during failed events reprocessing: {}", e.getMessage(), e);
        }
    }
}
