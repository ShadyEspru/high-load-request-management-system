package com.hlrms.apigateway.controller;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
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

    @RequestMapping("/request-service")
    public Mono<Map<String, Object>> requestServiceFallback(
            ServerWebExchange exchange
    ) {
        return fallbackResponse(
                exchange,
                "request-service",
                "Request service is temporarily unavailable."
        );
    }

    @RequestMapping("/auth-service")
    public Mono<Map<String, Object>> authServiceFallback(
            ServerWebExchange exchange
    ) {
        return fallbackResponse(
                exchange,
                "auth-service",
                "Authentication service is temporarily unavailable."
        );
    }

    private Mono<Map<String, Object>> fallbackResponse(
            ServerWebExchange exchange,
            String service,
            String message
    ) {
        exchange.getResponse().setStatusCode(
                HttpStatus.SERVICE_UNAVAILABLE
        );

        exchange.getResponse().getHeaders().setContentType(
                MediaType.APPLICATION_JSON
        );

        Throwable failure = exchange.getAttribute(
                ServerWebExchangeUtils
                        .CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 503);
        body.put("error", "Service Unavailable");
        body.put("service", service);
        body.put("message", message);

        String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Correlation-ID");

        if (correlationId != null) {
            body.put("correlationId", correlationId);
        }

        if (failure != null) {
            body.put(
                    "cause",
                    failure.getClass().getSimpleName()
            );
        }

        return Mono.just(body);
    }
}