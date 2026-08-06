package com.hlrms.apigateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayLoggingFilterTest {

    private GatewayLoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GatewayLoggingFilter();
    }

    @Test
    void shouldForwardRequestToFilterChain() {

        MockServerWebExchange exchange =
            createExchange(
                "/api/v1/requests",
                new InetSocketAddress(
                    "127.0.0.1",
                    54321
                )
            );

        exchange.getAttributes().put(
            CorrelationIdFilter
                .CORRELATION_ID_ATTRIBUTE,
            "correlation-123"
        );

        AtomicReference<ServerWebExchange>
            forwardedExchange =
            new AtomicReference<>();

        GatewayFilterChain chain =
            currentExchange -> {
                forwardedExchange.set(
                    currentExchange
                );

                currentExchange
                    .getResponse()
                    .setStatusCode(HttpStatus.OK);

                return Mono.empty();
            };

        StepVerifier.create(
            filter.filter(exchange, chain)
        )
        .verifyComplete();

        assertThat(forwardedExchange.get())
            .isSameAs(exchange);

        assertThat(
            exchange
                .getResponse()
                .getStatusCode()
        )
        .isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldCompleteWhenCorrelationIdIsMissing() {

        MockServerWebExchange exchange =
            createExchange(
                "/api/v1/requests",
                new InetSocketAddress(
                    "127.0.0.1",
                    54321
                )
            );

        AtomicBoolean chainCalled =
            new AtomicBoolean(false);

        GatewayFilterChain chain =
            currentExchange -> {
                chainCalled.set(true);

                currentExchange
                    .getResponse()
                    .setStatusCode(
                        HttpStatus.CREATED
                    );

                return Mono.empty();
            };

        StepVerifier.create(
            filter.filter(exchange, chain)
        )
        .verifyComplete();

        assertThat(chainCalled.get())
            .isTrue();

        assertThat(
            exchange
                .getResponse()
                .getStatusCode()
        )
        .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void shouldCompleteWhenRemoteAddressIsMissing() {

        MockServerWebExchange exchange =
            MockServerWebExchange.from(
                MockServerHttpRequest
                    .get("/api/v1/requests")
                    .build()
            );

        exchange.getAttributes().put(
            CorrelationIdFilter
                .CORRELATION_ID_ATTRIBUTE,
            "correlation-no-address"
        );

        AtomicBoolean chainCalled =
            new AtomicBoolean(false);

        GatewayFilterChain chain =
            currentExchange -> {
                chainCalled.set(true);
                return Mono.empty();
            };

        StepVerifier.create(
            filter.filter(exchange, chain)
        )
        .verifyComplete();

        assertThat(chainCalled.get())
            .isTrue();
    }

    @Test
    void shouldPropagateDownstreamError() {

        MockServerWebExchange exchange =
            createExchange(
                "/api/v1/requests",
                new InetSocketAddress(
                    "127.0.0.1",
                    54321
                )
            );

        exchange.getAttributes().put(
            CorrelationIdFilter
                .CORRELATION_ID_ATTRIBUTE,
            "correlation-error"
        );

        IllegalStateException failure =
            new IllegalStateException(
                "Downstream service failed"
            );

        GatewayFilterChain chain =
            currentExchange ->
                Mono.error(failure);

        StepVerifier.create(
            filter.filter(exchange, chain)
        )
        .expectErrorSatisfies(exception ->
            assertThat(exception)
                .isSameAs(failure)
        )
        .verify();
    }

    @Test
    void shouldHandleResponseWithoutStatusCode() {

        MockServerWebExchange exchange =
            createExchange(
                "/api/v1/requests",
                new InetSocketAddress(
                    "127.0.0.1",
                    54321
                )
            );

        GatewayFilterChain chain =
            currentExchange ->
                Mono.empty();

        StepVerifier.create(
            filter.filter(exchange, chain)
        )
        .verifyComplete();

        assertThat(
            exchange
                .getResponse()
                .getStatusCode()
        )
        .isNull();
    }

    @Test
    void shouldExposeExpectedFilterOrder() {

        assertThat(filter.getOrder())
            .isEqualTo(
                Ordered.HIGHEST_PRECEDENCE + 10
            );
    }

    private MockServerWebExchange createExchange(
        String path,
        InetSocketAddress remoteAddress
    ) {
        return MockServerWebExchange.from(
            MockServerHttpRequest
                .get(path)
                .remoteAddress(remoteAddress)
                .build()
        );
    }
}