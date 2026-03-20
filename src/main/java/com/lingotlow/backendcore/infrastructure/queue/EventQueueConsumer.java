package com.lingotlow.backendcore.infrastructure.queue;

import com.lingotlow.backendcore.infrastructure.metrics.TenantMetrics;
import com.lingotlow.backendcore.infrastructure.repository.EndpointRepository;
import com.lingotlow.backendcore.infrastructure.repository.EventRepository;
import com.lingotlow.backendcore.infrastructure.repository.entity.EndpointEntity;
import com.lingotlow.backendcore.infrastructure.repository.entity.EventEntity;
import com.lingotlow.backendcore.infrastructure.retry.RetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventQueueConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EventRepository eventRepository;
    private final EndpointRepository endpointRepository;
    private final TenantMetrics tenantMetrics;
    private final RetryService retryService;
    private final WebhookDeliveryService webhookDeliveryService;

    private static final String STREAM_KEY = "events:stream";
    private static final String CONSUMER_GROUP = "delivery-workers";
    private static final String CONSUMER_NAME = "worker-" + UUID.randomUUID().toString().substring(0, 8);

    @Scheduled(fixedDelay = 1000) // Process every second
    public void processEvents() {
        try {
            var records = redisTemplate.opsForStream()
                    .read(Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                            StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));

            if (!records.isEmpty()) {
                log.debug("Processing {} event records", records.size());
                
                for (var record : records) {
                    // Convert MapRecord to ObjectRecord
                    ObjectRecord<String, EventQueueProducer.EventQueueMessage> objectRecord = 
                        ObjectRecord.create(STREAM_KEY, (EventQueueProducer.EventQueueMessage) record.getValue());
                    processEventRecord(objectRecord);
                }
            }
        } catch (Exception e) {
            log.error("Error processing events from queue", e);
        }
    }

    private void processEventRecord(ObjectRecord<String, EventQueueProducer.EventQueueMessage> record) {
        try {
            EventQueueProducer.EventQueueMessage message = record.getValue();
            log.info("Processing event: tenantId={}, requestId={}, documentId={}", 
                    message.getTenantId(), message.getRequestId(), message.getDocumentId());

            // Get event from database
            EventEntity event = eventRepository.findByRequestId(message.getRequestId())
                    .orElseThrow(() -> new RuntimeException("Event not found: " + message.getRequestId()));

            // Get active endpoints for tenant
            List<EndpointEntity> endpoints = endpointRepository.findAllActiveByTenant(message.getTenantId());
            
            if (endpoints.isEmpty()) {
                log.warn("No active endpoints found for tenant: {}", message.getTenantId());
                updateEventStatus(event, EventEntity.EventStatus.FAILED, "No active endpoints");
                return;
            }

            // Deliver to all active endpoints
            boolean allDelivered = true;
            StringBuilder failureReasons = new StringBuilder();

            for (EndpointEntity endpoint : endpoints) {
                try {
                    boolean delivered = webhookDeliveryService.deliverWebhook(event, endpoint);
                    if (!delivered) {
                        allDelivered = false;
                        failureReasons.append("Failed to deliver to ").append(endpoint.getName()).append("; ");
                        
                        // Schedule retry if configured
                        if (event.getRetryCount() < endpoint.getRetryCount()) {
                            retryService.scheduleRetry(event, endpoint);
                        } else {
                            log.error("Max retries exceeded for event {} to endpoint {}", 
                                    event.getRequestId(), endpoint.getName());
                        }
                    }
                } catch (Exception e) {
                    allDelivered = false;
                    failureReasons.append("Error delivering to ").append(endpoint.getName())
                            .append(": ").append(e.getMessage()).append("; ");
                    log.error("Error delivering webhook to endpoint {}", endpoint.getName(), e);
                }
            }

            // Update event status
            if (allDelivered) {
                updateEventStatus(event, EventEntity.EventStatus.DELIVERED, null);
                tenantMetrics.incrementWebhookDelivered();
            } else {
                updateEventStatus(event, EventEntity.EventStatus.FAILED, failureReasons.toString());
                tenantMetrics.incrementWebhookFailed();
            }

            // Acknowledge the message
            redisTemplate.opsForStream().acknowledge(STREAM_KEY, CONSUMER_GROUP, record.getId());

        } catch (Exception e) {
            log.error("Error processing event record: {}", record.getId(), e);
            // Don't acknowledge - will be retried
        }
    }

    private void updateEventStatus(EventEntity event, EventEntity.EventStatus status, String failureReason) {
        event.setStatus(status);
        event.setFailureReason(failureReason);
        event.setUpdatedAt(java.time.OffsetDateTime.now());
        eventRepository.save(event);
        
        log.info("Event status updated: requestId={}, status={}, reason={}", 
                event.getRequestId(), status, failureReason);
    }

    // Initialize consumer group if it doesn't exist
    public void initializeConsumerGroup() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, CONSUMER_GROUP);
            log.info("Created consumer group: {}", CONSUMER_GROUP);
        } catch (Exception e) {
            // Group might already exist
            log.debug("Consumer group already exists or creation failed: {}", e.getMessage());
        }
    }
}
