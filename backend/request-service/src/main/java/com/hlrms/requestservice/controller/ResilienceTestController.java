package com.hlrms.requestservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/v1/resilience-test")
public class ResilienceTestController {

    private final Map<String, AtomicInteger> attempts =
            new ConcurrentHashMap<>();

    @GetMapping("/slow")
    public String slow(
            @RequestParam(defaultValue = "6000") long delayMs
    ) throws InterruptedException {
        Thread.sleep(delayMs);
        return "Completed after " + delayMs + " ms";
    }

    @GetMapping("/flaky")
    public ResponseEntity<Map<String, Object>> flaky(
            @RequestParam String testId,
            @RequestParam(defaultValue = "2") int failuresBeforeSuccess
    ) {
        int attempt = attempts
                .computeIfAbsent(testId, ignored -> new AtomicInteger())
                .incrementAndGet();

        Map<String, Object> body = Map.of(
                "testId", testId,
                "attempt", attempt,
                "failuresBeforeSuccess", failuresBeforeSuccess
        );

        if (attempt <= failuresBeforeSuccess) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(body);
        }

        attempts.remove(testId);

        return ResponseEntity.ok(body);
    }

    @GetMapping("/bulkhead")
    public Map<String, Object> bulkhead(
            @RequestParam(defaultValue = "2000") long delayMs
    ) throws InterruptedException {

        Thread.sleep(delayMs);

        return Map.of(
                "status", "completed",
                "delayMs", delayMs,
                "thread", Thread.currentThread().getName()
        );
    }
}