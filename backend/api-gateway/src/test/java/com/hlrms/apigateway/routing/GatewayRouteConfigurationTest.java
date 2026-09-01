package com.hlrms.apigateway.routing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "REQUEST_SERVICE_URL=http://localhost:18080",
        "AUTH_SERVICE_URL=http://localhost:18081",
        "SERVER_PORT=0"
    }
)
@ActiveProfiles("test")
class GatewayRouteConfigurationTest {

    @Autowired
    private RouteDefinitionLocator
        routeDefinitionLocator;

    @Test
    void shouldLoadExpectedRequestServiceRoutes() {

        List<RouteDefinition> routes =
            loadRoutes();

        assertThat(routes)
            .extracting(RouteDefinition::getId)
            .contains(
                "request-service-admin-route",
                "request-service-write-route",
                "request-service-read-route",
                "request-service-perf-ping-route",
                "request-service-perf-echo-route",
                "request-service-perf-bypass-route",
                "request-service-ping-route",
                "request-service-resilience-test-route",
                "request-service-bulkhead-test-route"
            );
    }

    @Test
    void shouldConfigureReadRequestServiceRoute() {

        RouteDefinition route =
            findRoute(
                "request-service-read-route"
            );

        assertThat(route.getUri())
            .isEqualTo(
                URI.create(
                    "http://localhost:18080"
                )
            );

        assertThat(route.getPredicates())
            .hasSize(2)
            .extracting(predicate ->
                predicate.getName()
            )
            .containsExactly(
                "Path",
                "Method"
            );

        assertThat(
            route.getPredicates()
                .getFirst()
                .getName()
        )
        .isEqualTo("Path");

        assertThat(
            route.getPredicates()
                .getFirst()
                .getArgs()
                .values()
        )
        .contains(
            "/api/v1/requests",
            "/api/v1/requests/**"
        );

        assertThat(
            route.getPredicates()
                .get(1)
                .getArgs()
                .values()
        )
        .contains("GET");
    }

    @Test
    void shouldConfigureWriteRequestServiceRoute() {

        RouteDefinition route =
            findRoute(
                "request-service-write-route"
            );

        assertThat(route.getUri())
            .isEqualTo(
                URI.create(
                    "http://localhost:18080"
                )
            );

        assertThat(route.getPredicates())
            .hasSize(2)
            .extracting(predicate ->
                predicate.getName()
            )
            .containsExactly(
                "Path",
                "Method"
            );

        assertThat(
            route.getPredicates()
                .getFirst()
                .getArgs()
                .values()
        )
        .containsExactly("/api/v1/requests");

        assertThat(
            route.getPredicates()
                .get(1)
                .getArgs()
                .values()
        )
        .contains("POST");

        assertThat(route.getFilters())
            .extracting(filter ->
                filter.getName()
            )
            .containsExactly(
                "AddResponseHeader"
            );
    }

    @Test
    void shouldConfigurePingRoute() {

        RouteDefinition route =
            findRoute(
                "request-service-ping-route"
            );

        assertThat(route.getUri())
            .isEqualTo(
                URI.create(
                    "http://localhost:18080"
                )
            );

        assertThat(route.getPredicates())
            .hasSize(1);

        assertThat(
            route.getPredicates()
                .getFirst()
                .getName()
        )
        .isEqualTo("Path");

        assertThat(
            route.getPredicates()
                .getFirst()
                .getArgs()
                .values()
        )
        .contains("/api/v1/ping");
    }

    @Test
    void shouldAddGatewayResponseHeaderToReadRoute() {

        RouteDefinition route =
            findRoute(
                "request-service-read-route"
            );

        assertThat(route.getFilters())
            .extracting(filter -> filter.getName())
            .contains(
                "RequestRateLimiter",
                "CircuitBreaker",
                "Retry",
                "AddResponseHeader"
            );

        var responseHeaderFilter =
            route.getFilters()
                .stream()
                .filter(filter ->
                    "AddResponseHeader".equals(
                        filter.getName()
                    )
                )
                .findFirst()
                .orElseThrow(
                    () -> new AssertionError(
                        "AddResponseHeader filter not found"
                    )
                );

        assertThat(
            responseHeaderFilter
                .getArgs()
                .values()
        )
        .contains(
            "X-Gateway-Service",
            "api-gateway"
        );
    }

    @Test
    void shouldAddGatewayResponseHeaderToPingRoute() {

        RouteDefinition route =
            findRoute(
                "request-service-ping-route"
            );

        assertThat(route.getFilters())
            .hasSize(1);

        assertThat(
            route.getFilters()
                .getFirst()
                .getName()
        )
        .isEqualTo("AddResponseHeader");

        assertThat(
            route.getFilters()
                .getFirst()
                .getArgs()
                .values()
        )
        .contains(
            "X-Gateway-Service",
            "api-gateway"
        );
    }

    @Test
    void shouldUseConfiguredRequestServiceUrlForAllRequestRoutes() {

        List<RouteDefinition> requestRoutes =
            loadRoutes()
                .stream()
                .filter(route ->
                    route.getId().startsWith(
                        "request-service"
                    )
                )
                .toList();

        assertThat(requestRoutes)
            .hasSize(9)
            .allSatisfy(route ->
                assertThat(route.getUri())
                    .isEqualTo(
                        URI.create(
                            "http://localhost:18080"
                        )
                    )
            );

        assertThat(requestRoutes)
            .extracting(RouteDefinition::getId)
            .containsExactlyInAnyOrder(
                "request-service-admin-route",
                "request-service-write-route",
                "request-service-read-route",
                "request-service-perf-ping-route",
                "request-service-perf-echo-route",
                "request-service-perf-bypass-route",
                "request-service-ping-route",
                "request-service-resilience-test-route",
                "request-service-bulkhead-test-route"
            );
    }
    @Test
    void shouldConfigureResilienceFiltersOnReadRequestRoute() {

        RouteDefinition route =
            findRoute(
                "request-service-read-route"
            );

        var filters =
            route.getFilters();

        var rateLimiter =
            filters.stream()
                .filter(filter ->
                    "RequestRateLimiter".equals(
                        filter.getName()
                    )
                )
                .findFirst()
                .orElseThrow();

        assertThat(rateLimiter.getArgs())
            .containsEntry(
                "redis-rate-limiter.replenishRate",
                "10"
            )
            .containsEntry(
                "redis-rate-limiter.burstCapacity",
                "20"
            )
            .containsEntry(
                "redis-rate-limiter.requestedTokens",
                "1"
            );

        var circuitBreaker =
            filters.stream()
                .filter(filter ->
                    "CircuitBreaker".equals(
                        filter.getName()
                    )
                )
                .findFirst()
                .orElseThrow();

        assertThat(
            circuitBreaker.getArgs()
        )
        .containsEntry(
            "name",
            "requestServiceCircuitBreaker"
        )
        .containsEntry(
            "fallbackUri",
            "forward:/fallback/request-service"
        );

        var retry =
            filters.stream()
                .filter(filter ->
                    "Retry".equals(
                        filter.getName()
                    )
                )
                .findFirst()
                .orElseThrow();

        assertThat(retry.getArgs())
            .containsEntry(
                "retries",
                "2"
            )
            .containsEntry(
                "methods.0",
                "GET"
            )
            .containsEntry(
                "backoff.firstBackoff",
                "100ms"
            )
            .containsEntry(
                "backoff.maxBackoff",
                "500ms"
            );
    }

    @Test
    void shouldLoadAuthServiceRoute() {

        RouteDefinition route =
            findRoute("auth-service-route");

        assertThat(route.getUri())
            .isEqualTo(
                URI.create(
                    "http://localhost:18081"
                )
            );

        assertThat(route.getPredicates())
            .hasSize(1);

        assertThat(
            route.getPredicates()
                .getFirst()
                .getName()
        )
        .isEqualTo("Path");

        assertThat(
            route.getPredicates()
                .getFirst()
                .getArgs()
                .values()
        )
        .contains(
            "/api/v1/auth",
            "/api/v1/auth/**"
        );
    }

    @Test
    void shouldConfigureAuthServiceCircuitBreaker() {

        RouteDefinition route =
            findRoute("auth-service-route");

        var circuitBreaker =
            route.getFilters()
                .stream()
                .filter(filter ->
                    "CircuitBreaker".equals(
                        filter.getName()
                    )
                )
                .findFirst()
                .orElseThrow(
                    () -> new AssertionError(
                        "Auth CircuitBreaker filter was not found"
                    )
                );

        assertThat(
            circuitBreaker.getArgs()
        )
        .containsEntry(
            "name",
            "authServiceCircuitBreaker"
        )
        .containsEntry(
            "fallbackUri",
            "forward:/fallback/auth-service"
        );
    }

    @Test
    void shouldAddGatewayHeaderToAuthRoute() {

        RouteDefinition route =
            findRoute("auth-service-route");

        var responseHeader =
            route.getFilters()
                .stream()
                .filter(filter ->
                    "AddResponseHeader".equals(
                        filter.getName()
                    )
                )
                .findFirst()
                .orElseThrow(
                    () -> new AssertionError(
                        "Auth AddResponseHeader filter was not found"
                    )
                );

        assertThat(
            responseHeader
                .getArgs()
                .values()
        )
        .contains(
            "X-Gateway-Service",
            "api-gateway"
        );
    }

    private RouteDefinition findRoute(
        String routeId
    ) {
        return loadRoutes()
            .stream()
            .filter(route ->
                routeId.equals(route.getId())
            )
            .findFirst()
            .orElseThrow(
                () -> new AssertionError(
                    "Route was not loaded: "
                        + routeId
                )
            );
    }

    private List<RouteDefinition> loadRoutes() {

        return Flux
            .from(
                routeDefinitionLocator
                    .getRouteDefinitions()
            )
            .collectList()
            .blockOptional()
            .orElse(List.of());
    }
}
