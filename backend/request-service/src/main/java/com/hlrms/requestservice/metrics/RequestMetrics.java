package com.hlrms.requestservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class RequestMetrics {

    private final Counter requestsCreated;

    private final Counter requestsCompleted;

    private final Counter requestsFailed;

    private final Counter requestsReplayed;

    private final Counter requestsCacheHit;

    public RequestMetrics(
            MeterRegistry meterRegistry
    ) {

        requestsCreated =
                Counter.builder("hlrms.requests.creation")
                        .description("Created requests")
                        .register(meterRegistry);

        requestsCompleted =
                Counter.builder("hlrms.requests.completed")
                        .description("Completed requests")
                        .register(meterRegistry);

        requestsFailed =
                Counter.builder("hlrms.requests.failed")
                        .description("Failed requests")
                        .register(meterRegistry);

        requestsReplayed =
                Counter.builder("hlrms.requests.replayed")
                        .description("Idempotency replay")
                        .register(meterRegistry);

        requestsCacheHit =
                Counter.builder("hlrms.requests.cache.hit")
                        .description("Redis cache hits")
                        .register(meterRegistry);
    }

    public void requestCreated() {
        requestsCreated.increment();
    }

    public void requestCompleted() {
        requestsCompleted.increment();
    }

    public void requestFailed() {
        requestsFailed.increment();
    }

    public void replayed() {
        requestsReplayed.increment();
    }

    public void cacheHit() {
        requestsCacheHit.increment();
    }
}