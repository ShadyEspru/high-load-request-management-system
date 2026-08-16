package com.hlrms.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Slf4j
// @Component  // temporarily disabled for gateway performance isolation
public class GatewayLoggingFilter
    implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(
        ServerWebExchange exchange,
        GatewayFilterChain chain
    ) {
        Instant startedAt = Instant.now();

        String method =
            exchange
                .getRequest()
                .getMethod()
                .name();

        String path =
            exchange
                .getRequest()
                .getURI()
                .getRawPath();

        String correlationId =
            exchange.getAttributeOrDefault(
                CorrelationIdFilter
                    .CORRELATION_ID_ATTRIBUTE,
                "unknown"
            );

        String remoteAddress =
            resolveRemoteAddress(exchange);

        log.info(
            "Gateway request started. " +
            "method={}, path={}, remoteAddress={}, " +
            "correlationId={}",
            method,
            path,
            remoteAddress,
            correlationId
        );

        return chain
            .filter(exchange)
            .doOnError(
                exception ->
                    log.error(
                        "Gateway request failed. " +
                        "method={}, path={}, " +
                        "correlationId={}, errorType={}, " +
                        "message={}",
                        method,
                        path,
                        correlationId,
                        exception
                            .getClass()
                            .getSimpleName(),
                        exception.getMessage()
                    )
            )
            .doFinally(signalType -> {
                long durationMs =
                    Duration
                        .between(
                            startedAt,
                            Instant.now()
                        )
                        .toMillis();

                HttpStatusCode statusCode =
                    exchange
                        .getResponse()
                        .getStatusCode();

                int status =
                    statusCode == null
                        ? 0
                        : statusCode.value();

                log.info(
                    "Gateway request completed. " +
                    "method={}, path={}, status={}, " +
                    "durationMs={}, correlationId={}, " +
                    "signal={}",
                    method,
                    path,
                    status,
                    durationMs,
                    correlationId,
                    signalType
                );
            });
    }

    private String resolveRemoteAddress(
        ServerWebExchange exchange
    ) {
        if (
            exchange
                .getRequest()
                .getRemoteAddress() == null
        ) {
            return "unknown";
        }

        return exchange
            .getRequest()
            .getRemoteAddress()
            .getAddress()
            .getHostAddress();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}