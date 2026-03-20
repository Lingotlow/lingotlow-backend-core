package com.lingotlow.backendcore.infrastructure.retry;

import com.lingotlow.backendcore.infrastructure.metrics.TenantMetrics;
import com.lingotlow.backendcore.infrastructure.queue.EventQueueProducer;
import com.lingotlow.backendcore.infrastructure.repository.entity.EventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EventQueueProducer eventQueueProducer;
    private final TenantMetrics tenantMetrics;

    private static final String RETRY_QUEUE_KEY = "events:retry";
    private static final String DLQ_KEY = "events:dlq";
    private static final String RETRY_COUNT_KEY = "events:retry:count:";
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_DELAY_MS = 1000; // 1 second
    private static final double BACKOFF_MULTIPLIER = 2.0;

    public void scheduleRetry(EventEntity event, com.lingotlow.backendcore.infrastructure.repository.entity.EndpointEntity endpoint) {
        try {
            String retryKey = RETRY_COUNT_KEY + event.getRequestId();
            Integer retryCount = (Integer) redisTemplate.opsForValue().get(retryKey);
            
            if (retryCount == null) {
                retryCount = 0;
            }
            retryCount++;

            if (retryCount > MAX_RETRIES) {
                log.warn("Max retries exceeded for event: {}, moving to DLQ", event.getRequestId());
                moveToDeadLetterQueue(event, "Max retries exceeded");
                return;
            }

            // Calculate delay with exponential backoff
            long delayMs = (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, retryCount - 1));
            Instant retryAt = Instant.now().plusMillis(delayMs);

            // Store retry count
            redisTemplate.opsForValue().set(retryKey, retryCount, Duration.ofHours(24));

            // Add to retry queue with timestamp
            RetryEntry retryEntry = new RetryEntry(
                    event.getRequestId(),
                    event.getTenantId(),
                    endpoint.getId(),
                    retryAt.toEpochMilli(),
                    retryCount
            );

            redisTemplate.opsForZSet().add(RETRY_QUEUE_KEY, retryEntry, retryAt.toEpochMilli());
            
            log.info("Scheduled retry for event: {}, attempt {}, delay: {}ms", 
                    event.getRequestId(), retryCount, delayMs);

            tenantMetrics.incrementWebhookRetried();

        } catch (Exception e) {
            log.error("Error scheduling retry for event: {}", event.getRequestId(), e);
        }
    }

    @Scheduled(fixedDelay = 5000) // Check every 5 seconds
    public void processRetries() {
        try {
            long currentTime = System.currentTimeMillis();
            
            // Get all retries that are ready to be processed
            Set<Object> readyRetries = redisTemplate.opsForZSet()
                    .rangeByScore(RETRY_QUEUE_KEY, 0, currentTime);

            if (!readyRetries.isEmpty()) {
                log.debug("Processing {} retry entries", readyRetries.size());

                for (Object retryObj : readyRetries) {
                    RetryEntry retry = (RetryEntry) retryObj;
                    processRetry(retry);
                }
            }
        } catch (Exception e) {
            log.error("Error processing retries", e);
        }
    }

    private void processRetry(RetryEntry retry) {
        try {
            log.info("Processing retry: eventId={}, attempt={}", retry.getEventId(), retry.getRetryCount());

            // Re-enqueue the event for processing
            EventQueueProducer.EventQueueMessage message = new EventQueueProducer.EventQueueMessage(
                    retry.getEventId(),
                    retry.getTenantId(),
                    null, // documentId will be loaded from database
                    null, // type will be loaded from database
                    "RETRY",
                    null
            );

            boolean enqueued = eventQueueProducer.enqueueEventWithIdempotencyCheck(message);
            
            if (enqueued) {
                // Remove from retry queue
                redisTemplate.opsForZSet().remove(RETRY_QUEUE_KEY, retry);
                log.info("Retry event re-enqueued successfully: {}", retry.getEventId());
            } else {
                log.warn("Failed to re-enqueue retry event: {}", retry.getEventId());
            }

        } catch (Exception e) {
            log.error("Error processing retry for event: {}", retry.getEventId(), e);
        }
    }

    private void moveToDeadLetterQueue(EventEntity event, String reason) {
        try {
            DeadLetterEntry dlqEntry = new DeadLetterEntry(
                    event.getRequestId(),
                    event.getTenantId(),
                    event.getDocumentId(),
                    event.getType(),
                    reason,
                    System.currentTimeMillis()
            );

            redisTemplate.opsForList().rightPush(DLQ_KEY, dlqEntry);
            
            // Clean up retry count
            redisTemplate.delete(RETRY_COUNT_KEY + event.getRequestId());
            
            log.warn("Event moved to DLQ: {}, reason: {}", event.getRequestId(), reason);

        } catch (Exception e) {
            log.error("Error moving event to DLQ: {}", event.getRequestId(), e);
        }
    }

    // DTOs for Redis storage
    public static class RetryEntry {
        private final UUID eventId;
        private final UUID tenantId;
        private final UUID endpointId;
        private final long retryAt;
        private final int retryCount;

        public RetryEntry(UUID eventId, UUID tenantId, UUID endpointId, long retryAt, int retryCount) {
            this.eventId = eventId;
            this.tenantId = tenantId;
            this.endpointId = endpointId;
            this.retryAt = retryAt;
            this.retryCount = retryCount;
        }

        // Getters
        public UUID getEventId() { return eventId; }
        public UUID getTenantId() { return tenantId; }
        public UUID getEndpointId() { return endpointId; }
        public long getRetryAt() { return retryAt; }
        public int getRetryCount() { return retryCount; }
    }

    public static class DeadLetterEntry {
        private final UUID eventId;
        private final UUID tenantId;
        private final String documentId;
        private final String eventType;
        private final String reason;
        private final long timestamp;

        public DeadLetterEntry(UUID eventId, UUID tenantId, String documentId, String eventType, String reason, long timestamp) {
            this.eventId = eventId;
            this.tenantId = tenantId;
            this.documentId = documentId;
            this.eventType = eventType;
            this.reason = reason;
            this.timestamp = timestamp;
        }

        // Getters
        public UUID getEventId() { return eventId; }
        public UUID getTenantId() { return tenantId; }
        public String getDocumentId() { return documentId; }
        public String getEventType() { return eventType; }
        public String getReason() { return reason; }
        public long getTimestamp() { return timestamp; }
    }
}
