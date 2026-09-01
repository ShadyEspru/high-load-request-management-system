package com.hlrms.requestworker.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class WorkerMetrics {

    private final Counter requestsCompleted;
    private final Counter requestsFailed;
    private final Timer processingTimer;

    public WorkerMetrics(MeterRegistry meterRegistry) {

        this.requestsCompleted =
                Counter.builder("hlrms.requests.completed")
                        .description("Requests completed successfully by worker")
                        .register(meterRegistry);


        this.requestsFailed =
                Counter.builder("hlrms.requests.failed")
                        .description("Requests that failed during worker processing")
                        .register(meterRegistry);


        this.processingTimer =
                Timer.builder("hlrms.request.processing")
                        .description("Request processing time")
                        .publishPercentiles(0.50, 0.95, 0.99)
                        .register(meterRegistry);
    }


    public void requestCompleted() {
        requestsCompleted.increment();
    }


    public void requestFailed() {
        requestsFailed.increment();
    }


    public Timer.Sample startProcessingTimer(MeterRegistry registry) {
        return Timer.start(registry);
    }


    public void recordProcessingTime(Timer.Sample sample) {
        sample.stop(processingTimer);
    }
}