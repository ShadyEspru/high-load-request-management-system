package com.hlrms.apigateway.controller;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class ResilienceFallbackController {

    private static final int RETRY_AFTER_SECONDS = 10;

    @RequestMapping("/request-service")
    public Mono<Map<String, Object>> requestServiceFallback(
            ServerWebExchange exchange
    ) {
        return fallbackResponse(
                exchange,
                "request-service",
                "Request service is temporarily unavailable.",
                true
        );
    }

    @RequestMapping("/auth-service")
    public Mono<Map<String, Object>> authServiceFallback(
            ServerWebExchange exchange
    ) {
        return fallbackResponse(
                exchange,
                "auth-service",
                "Authentication service is temporarily unavailable.",
                true
        );
    }

    private Mono<Map<String, Object>> fallbackResponse(
            ServerWebExchange exchange,
            String service,
            String message,
            boolean retryable
    ) {
        exchange.getResponse().setStatusCode(
                HttpStatus.SERVICE_UNAVAILABLE
        );

        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(RETRY_AFTER_SECONDS));
        headers.set("X-System-Degraded", "true");
        headers.set("X-Fallback-Service", service);

        Throwable failure = exchange.getAttribute(
                ServerWebExchangeUtils
                        .CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR
        );

        String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Correlation-ID");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 503);
        body.put("error", "Service Unavailable");
        body.put("service", service);
        body.put("degraded", true);
        body.put("retryable", retryable);
        body.put("retryAfterSeconds", RETRY_AFTER_SECONDS);
        body.put("message", message);
        body.put("path", exchange.getRequest().getPath().value());
        body.put("method", exchange.getRequest().getMethod().name());

        if (correlationId != null && !correlationId.isBlank()) {
            body.put("correlationId", correlationId);
        }

        if (failure != null) {
            body.put("cause", failure.getClass().getSimpleName());
        }

        return Mono.just(body);
    }
}