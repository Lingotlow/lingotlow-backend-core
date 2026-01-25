package com.lingotlow.backendcore.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TenantMetrics {

    private final Counter tenantCreateCounter;
    private final Counter tenantUpdateCounter;
    private final Counter tenantDeleteCounter;
    private final Counter tenantReadCounter;
    private final Counter tenantListCounter;
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
}
