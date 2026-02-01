package com.lingotlow.backendcore.infrastructure.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventQueueProducer {

    private static final String STREAM_NAME = "events:stream";
    private static final String COUNTER_ENQUEUED = "ingest_enqueued_total";
    private static final String COUNTER_REJECTED = "ingest_rejected_total";
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public boolean enqueueEvent(EventQueueMessage message) {
        try {
            log.debug("Enqueuing event to stream: tenantId={}, requestId={}",
                    message.getTenantId(), message.getRequestId());

            // Create stream record
            Map<String, String> fields = new HashMap<>();
            fields.put("requestId", message.getRequestId().toString());
            fields.put("tenantId", message.getTenantId().toString());
            fields.put("documentId", message.getDocumentId());
            fields.put("type", message.getType());
            fields.put("status", message.getStatus());
            fields.put("timestamp", Instant.now().toString());
            fields.put("payload", objectMapper.writeValueAsString(message.getPayload()));

            // Add additional metadata
            if (message.getHeaders() != null) {
                fields.put("headers", objectMapper.writeValueAsString(message.getHeaders()));
            }

            fields.put("attempts", String.valueOf(message.getAttempts()));
            fields.put("enqueuedAt", Instant.now().toString());

            StringRecord record = StreamRecords.newRecord()
                    .ofStrings(fields)
                    .withStreamKey(STREAM_NAME);

            // Add to Redis Stream
            RecordId recordId = redisTemplate.opsForStream().add(record);

            if (recordId != null) {
                log.info("Event enqueued successfully: tenantId={}, requestId={}, recordId={}",
                        message.getTenantId(), message.getRequestId(), recordId);

                // Increment success metric
                getEnqueuedCounter().increment();
                return true;
            } else {
                log.error("Failed to enqueue event: tenantId={}, requestId={}",
                        message.getTenantId(), message.getRequestId());
                getRejectedCounter().increment();
                return false;
            }

        } catch (Exception e) {
            log.error("Error enqueuing event to stream: tenantId={}, requestId={}, error={}",
                    message.getTenantId(), message.getRequestId(), e.getMessage(), e);
            getRejectedCounter().increment();
            return false;
        }
    }

    public boolean enqueueEventWithRetry(EventQueueMessage message, int maxRetries) {
        int attempts = 0;

        while (attempts < maxRetries) {
            if (enqueueEvent(message)) {
                return true;
            }

            attempts++;
            log.warn("Retry attempt {} for event enqueuing: tenantId={}, requestId={}",
                    attempts, message.getTenantId(), message.getRequestId());

            if (attempts < maxRetries) {
                try {
                    // Exponential backoff: 100ms, 200ms, 400ms
                    Thread.sleep(100 * (1L << (attempts - 1)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Thread interrupted during retry for event: tenantId={}, requestId={}",
                            message.getTenantId(), message.getRequestId());
                    break;
                }
            }
        }

        log.error("Failed to enqueue event after {} attempts: tenantId={}, requestId={}",
                maxRetries, message.getTenantId(), message.getRequestId());

        // Increment rejection metric
        getRejectedCounter().increment();

        return false;
    }

    public boolean enqueueEventWithIdempotencyCheck(EventQueueMessage message) {
        try {
            // Check if message was already processed (idempotency)
            String idempotencyKey = "event:" + message.getRequestId().toString();

            // Use Redis to check idempotency
            Boolean exists = redisTemplate.hasKey(idempotencyKey);
            if (exists) {
                log.info("Event already processed (idempotent): requestId={}", message.getRequestId());
                return true; // Success, but don't process again
            }

            // Try to enqueue
            boolean enqueued = enqueueEventWithRetry(message, 3);

            if (enqueued) {
                // Mark as processed for idempotency (expires in 24 hours)
                redisTemplate.opsForValue().set(idempotencyKey, "processed", 86400);
            }

            return enqueued;

        } catch (Exception e) {
            log.error("Error in idempotent enqueue for requestId: {}, error: {}",
                    message.getRequestId(), e.getMessage(), e);
            return false;
        }
    }

    public boolean isQueueHealthy() {
        try {
            redisTemplate.opsForStream().info(STREAM_NAME);
            return true;
        } catch (Exception e) {
            log.warn("Queue health check failed: {}", e.getMessage());
            return false;
        }
    }

    private Counter getEnqueuedCounter() {
        return Counter.builder(COUNTER_ENQUEUED)
                .description("Total number of events enqueued")
                .register(meterRegistry);
    }

    private Counter getRejectedCounter() {
        return Counter.builder(COUNTER_REJECTED)
                .description("Total number of events rejected")
                .register(meterRegistry);
    }

    public static class EventQueueMessage {
        private UUID requestId;
        private UUID tenantId;
        private String documentId;
        private String type;
        private String status;
        private Map<String, Object> payload;
        private Map<String, String> headers;
        private int attempts;

        // Constructors
        public EventQueueMessage() {
        }

        public EventQueueMessage(UUID requestId, UUID tenantId, String documentId,
                                 String type, String status, Map<String, Object> payload) {
            this.requestId = requestId;
            this.tenantId = tenantId;
            this.documentId = documentId;
            this.type = type;
            this.status = status;
            this.payload = payload;
            this.attempts = 0;
        }

        // Getters and Setters
        public UUID getRequestId() {
            return requestId;
        }

        public void setRequestId(UUID requestId) {
            this.requestId = requestId;
        }

        public UUID getTenantId() {
            return tenantId;
        }

        public void setTenantId(UUID tenantId) {
            this.tenantId = tenantId;
        }

        public String getDocumentId() {
            return documentId;
        }

        public void setDocumentId(String documentId) {
            this.documentId = documentId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }

        public void setPayload(Map<String, Object> payload) {
            this.payload = payload;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }
    }
}
