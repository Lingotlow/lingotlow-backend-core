package com.lingotlow.backendcore.infrastructure.metrics;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class TenantMetrics {

    private final Counter tenantCreateCounter;
    private final Counter tenantUpdateCounter;
    private final Counter tenantDeleteCounter;
    private final Counter tenantReadCounter;
    private final Counter tenantListCounter;
    
    // Webhook operations counters
    private final Counter webhooksReceivedCounter;
    private final Counter webhooksDeliveredCounter;
    private final Counter webhooksFailedCounter;
    private final Counter webhooksRetriedCounter;
    
    private final MeterRegistry meterRegistry;

    public TenantMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.tenantCreateCounter = Counter.builder("tenant.operations.create")
                .description("Total number of tenant creation operations")
                .register(meterRegistry);
                
        this.tenantUpdateCounter = Counter.builder("tenant.operations.update")
                .description("Total number of tenant update operations")
                .register(meterRegistry);
                
        this.tenantDeleteCounter = Counter.builder("tenant.operations.delete")
                .description("Total number of tenant deletion operations")
                .register(meterRegistry);
                
        this.tenantReadCounter = Counter.builder("tenant.operations.read")
                .description("Total number of tenant read operations")
                .register(meterRegistry);
                
        this.tenantListCounter = Counter.builder("tenant.operations.list")
                .description("Total number of tenant list operations")
                .register(meterRegistry);
                
        // Initialize webhook counters
        this.webhooksReceivedCounter = Counter.builder("webhooks_received_total")
                .description("Total number of webhooks received")
                .register(meterRegistry);

        this.webhooksDeliveredCounter = Counter.builder("webhooks_delivered_total")
                .description("Total number of webhooks successfully delivered")
                .register(meterRegistry);

        this.webhooksFailedCounter = Counter.builder("webhooks_failed_total")
                .description("Total number of webhook deliveries failed")
                .register(meterRegistry);

        this.webhooksRetriedCounter = Counter.builder("webhooks_retried_total")
                .description("Total number of webhook retries attempted")
                .register(meterRegistry);
    }

    public void incrementTenantCreate() {
        tenantCreateCounter.increment();
        log.debug("Tenant create counter incremented");
    }

    public void incrementTenantUpdate() {
        tenantUpdateCounter.increment();
        log.debug("Tenant update counter incremented");
    }

    public void incrementTenantDelete() {
        tenantDeleteCounter.increment();
        log.debug("Tenant delete counter incremented");
    }

    public void incrementTenantRead() {
        tenantReadCounter.increment();
        log.debug("Tenant read counter incremented");
    }

    public void incrementTenantList() {
        tenantListCounter.increment();
        log.debug("Tenant list counter incremented");
    }

    // Webhook operations
    public void incrementWebhooksReceived() {
        webhooksReceivedCounter.increment();
        log.debug("Webhooks received counter incremented");
    }

    public void incrementWebhookDelivered() {
        webhooksDeliveredCounter.increment();
        log.debug("Webhook delivered counter incremented");
    }

    public void incrementWebhookFailed() {
        webhooksFailedCounter.increment();
        log.debug("Webhook failed counter incremented");
    }

    public void incrementWebhookRetried() {
        webhooksRetriedCounter.increment();
        log.debug("Webhook retried counter incremented");
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordTimer(Timer.Sample sample) {
        sample.stop(Timer.builder("tenant.operation.duration")
                .description("Duration of tenant operations")
                .register(meterRegistry));
    }

    public void recordTimer(Timer.Sample sample, String operation, String result) {
        sample.stop(Timer.builder("tenant.operation.duration")
                .description("Duration of tenant operations")
                .tag("operation", operation)
                .tag("result", result)
                .register(meterRegistry));
    }
    
    // Additional metrics for monitoring
    public void recordWebhookDeliveryTime(Duration duration, String status) {
        Timer.builder("webhook_delivery_duration_seconds")
                .tag("status", status)
                .register(meterRegistry)
                .record(duration);
    }

    // TODO: Implement queue size and active endpoints metrics when Gauge API is fixed
    public void recordQueueSize(String queueName, int size) {
        log.debug("Queue size for {}: {}", queueName, size);
    }

    public void recordActiveEndpoints(int count) {
        log.debug("Active endpoints: {}", count);
    }
}
