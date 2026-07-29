package com.hlrms.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdFilter
    implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER =
        "X-Correlation-ID";

    public static final String CORRELATION_ID_ATTRIBUTE =
        "hlrms.correlationId";

    @Override
    public Mono<Void> filter(
        ServerWebExchange exchange,
        GatewayFilterChain chain
    ) {
        String correlationId =
            resolveCorrelationId(exchange);

        ServerHttpRequest request =
            exchange
                .getRequest()
                .mutate()
                .headers(
                    headers -> headers.set(
                        CORRELATION_ID_HEADER,
                        correlationId
                    )
                )
                .build();

        ServerWebExchange mutatedExchange =
            exchange
                .mutate()
                .request(request)
                .build();

        mutatedExchange
            .getAttributes()
            .put(
                CORRELATION_ID_ATTRIBUTE,
                correlationId
            );

        mutatedExchange
            .getResponse()
            .getHeaders()
            .set(
                CORRELATION_ID_HEADER,
                correlationId
            );

        return Mono.defer(() -> {
            MDC.put(
                "correlationId",
                correlationId
            );

            return chain
                .filter(mutatedExchange)
                .doFinally(
                    signalType ->
                        MDC.remove("correlationId")
                );
        });
    }

    private String resolveCorrelationId(
        ServerWebExchange exchange
    ) {
        String incomingCorrelationId =
            exchange
                .getRequest()
                .getHeaders()
                .getFirst(
                    CORRELATION_ID_HEADER
                );

        if (
            StringUtils.hasText(
                incomingCorrelationId
            )
        ) {
            return sanitizeCorrelationId(
                incomingCorrelationId
            );
        }

        return UUID.randomUUID().toString();
    }

    private String sanitizeCorrelationId(
        String correlationId
    ) {
        String normalized =
            correlationId.trim();

        if (
            normalized.length() > 100
                || !normalized.matches(
                    "[A-Za-z0-9._\\-]+"
                )
        ) {
            log.warn(
                "Invalid incoming correlation ID was " +
                "replaced. valueLength={}",
                normalized.length()
            );

            return UUID.randomUUID().toString();
        }

        return normalized;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}