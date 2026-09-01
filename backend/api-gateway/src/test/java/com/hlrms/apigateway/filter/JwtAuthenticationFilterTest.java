package com.hlrms.apigateway.filter;

import com.hlrms.apigateway.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String USER_ID =
        "3b4b8547-9fe0-4d80-81e4-b6579346da8c";

    @Mock
    private JwtService jwtService;

    @Mock
    private Claims claims;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter =
            new JwtAuthenticationFilter(
                jwtService
            );
    }

    @Test
    void shouldAllowAuthPathWithoutToken() {

        MockServerWebExchange exchange =
            exchangeFor("/api/v1/auth/login");

        AtomicReference<ServerWebExchange>
            forwardedExchange =
            new AtomicReference<>();

        GatewayFilterChain chain =
            currentExchange -> {
                forwardedExchange.set(
                    currentExchange
                );

                return Mono.empty();
            };

        StepVerifier.create(
            filter.filter(exchange, chain)
        )
        .verifyComplete();

        assertThat(forwardedExchange.get())
            .isSameAs(exchange);

        verify(
            jwtService,
            never()
        )
        .validateAndExtractClaims(
            org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void shouldAllowActuatorPathWithoutToken() {

        MockServerWebExchange exchange =
            exchangeFor("/actuator/health");

        AtomicReference<ServerWebExchange>
            forwardedExchange =
            new AtomicReference<>();

        GatewayFilterChain chain =
            currentExchange -> {
                forwardedExchange.set(
                    currentExchange
                );

                return Mono.empty();
            };

        StepVerifier.create(
            filter.filter(exchange, chain)
        )
        .verifyComplete();

        assertThat(forwardedExchange.get())
            .isSameAs(exchange);
    }

    @Test
    void shouldAllowFallbackPathWithoutToken() {

        MockServerWebExchange exchange =
            exchangeFor(
                "/fallback/request-service"
            );

        AtomicReference<ServerWebExchange>
            forwardedExchange =
            new AtomicReference<>();

        GatewayFilterChain chain =
            currentExchange -> {
                forwardedExchange.set(
                    currentExchange
                );

                return Mono.empty();
            };

        StepVerifier.create(
            filter.filter(exchange, chain)
        )
        .verifyComplete();

        assertThat(forwardedExchange.get())
            .isSameAs(exchange);
    }

    @Test
    void shouldRejectMissingAuthorizationHeader() {

        MockServerWebExchange exchange =
            exchangeFor("/api/v1/requests");

        GatewayFilterChain chain =
            currentExchange ->
                Mono.error(
                    new AssertionError(
                        "Chain must not be called"
                    )
                );

        StepVerifier.create(
            filter.filter(exchange, chain)
        )
        .verifyComplete();

        assertThat(
            exchange
                .getResponse()
                .getStatusCode()
        )
        .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(
            exchange
                .getResponse()
                .getHeaders()
                .getContentType()
        )
        .hasToString("application/json");

        assertThat(responseBody(exchange))
            .contains(
                "\"status\": 401"
            )
            .contains(
                "\"error\": \"Unauthorized\""
            )
            .contains(
                "Missing or invalid Authorization header."
            );

        verify(
            jwtService,
            never()
        )
        .validateAndExtractClaims(
            org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void shouldRejectAuthorizationWithoutBearerPrefix() {

        MockServerWebExchange exchange =
            MockServerWebExchange.from(
                MockServerHttpRequest
                    .get("/api/v1/requests")
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "Basic abc123"
                    )
                    .build()
            );

        GatewayFilterChain chain =
            currentExchange ->
                Mono.error(
                    new AssertionError(
                        "Chain must not be called"
                    )
                );

        StepVerifier.create(
            filter.filter(exchange, chain)
        )
        .verifyComplete();

        assertThat(
            exchange
                .getResponse()
                .getStatusCode()
        )
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectInvalidToken() {

        MockServerWebExchange exchange =
            authenticatedExchange(
                "/api/v1/requests",
                "invalid-token"
            );

        when(
            jwtService.validateAndExtractClaims(
                "invalid-token"
            )
        )
        .thenThrow(
            new io.jsonwebtoken.JwtException(
                "invalid token"
            )
        );

        GatewayFilterChain chain =
            currentExchange ->
                Mono.error(
                    new AssertionError(
                        "Chain must not be called"
                    )
                );

        StepVerifier.create(
            filter.filter(exchange, chain)
        )
        .verifyComplete();

        assertThat(
            exchange
                .getResponse()
                .getStatusCode()
        )
        .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(responseBody(exchange))
            .contains(
                "Invalid or expired access token."
            );

        verify(
            jwtService
        )
        .validateAndExtractClaims(
            "invalid-token"
        );
    }

    @Test
    void shouldInjectTrustedIdentityHeaders() {

        MockServerWebExchange exchange =
            authenticatedExchange(
                "/api/v1/requests",
                "valid-token"
            );

        when(
            jwtService.validateAndExtractClaims(
                "valid-token"
            )
        )
        .thenReturn(claims);

        when(
            jwtService.extractUserId(claims)
        )
        .thenReturn(USER_ID);

        when(
            jwtService.extractEmail(claims)
        )
        .thenReturn("user@example.com");

        when(
            jwtService.extractRoles(claims)
        )
        .thenReturn(
            List.of("USER", "ADMIN")
        );

        AtomicReference<ServerWebExchange>
            forwardedExchange =
            new AtomicReference<>();

        GatewayFilterChain chain =
            currentExchange -> {
                forwardedExchange.set(
                    currentExchange
                );

                return Mono.empty();
            };

        StepVerifier.create(
            filter.filter(exchange, chain)
        )
        .verifyComplete();

        ServerWebExchange forwarded =
            forwardedExchange.get();

        assertThat(forwarded)
            .isNotNull();

        assertThat(
            forwarded
                .getRequest()
                .getHeaders()
                .getFirst("X-User-Id")
        )
        .isEqualTo(USER_ID);

        assertThat(
            forwarded
                .getRequest()
                .getHeaders()
                .getFirst("X-User-Email")
        )
        .isEqualTo("user@example.com");

        assertThat(
            forwarded
                .getRequest()
                .getHeaders()
                .getFirst("X-User-Roles")
        )
        .isEqualTo("USER,ADMIN");
    }

    @Test
    void shouldReplaceClientSuppliedIdentityHeaders() {

        MockServerWebExchange exchange =
            MockServerWebExchange.from(
                MockServerHttpRequest
                    .get("/api/v1/requests")
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer valid-token"
                    )
                    .header(
                        "X-User-Id",
                        "forged-user-id"
                    )
                    .header(
                        "X-User-Email",
                        "attacker@example.com"
                    )
                    .header(
                        "X-User-Roles",
                        "ADMIN"
                    )
                    .build()
            );

        when(
            jwtService.validateAndExtractClaims(
                "valid-token"
            )
        )
        .thenReturn(claims);

        when(
            jwtService.extractUserId(claims)
        )
        .thenReturn(USER_ID);

        when(
            jwtService.extractEmail(claims)
        )
        .thenReturn("real@example.com");

        when(
            jwtService.extractRoles(claims)
        )
        .thenReturn(List.of("USER"));

        AtomicReference<ServerWebExchange>
            forwardedExchange =
            new AtomicReference<>();

        StepVerifier.create(
            filter.filter(
                exchange,
                currentExchange -> {
                    forwardedExchange.set(
                        currentExchange
                    );

                    return Mono.empty();
                }
            )
        )
        .verifyComplete();

        HttpHeaders forwardedHeaders =
            forwardedExchange
                .get()
                .getRequest()
                .getHeaders();

        assertThat(
            forwardedHeaders.get("X-User-Id")
        )
        .containsExactly(USER_ID);

        assertThat(
            forwardedHeaders.get("X-User-Email")
        )
        .containsExactly(
            "real@example.com"
        );

        assertThat(
            forwardedHeaders.get("X-User-Roles")
        )
        .containsExactly("USER");

        assertThat(forwardedHeaders.toString())
            .doesNotContain(
                "forged-user-id",
                "attacker@example.com"
            );
    }

    @Test
    void shouldForwardEmptyRolesHeaderWhenTokenHasNoRoles() {

        MockServerWebExchange exchange =
            authenticatedExchange(
                "/api/v1/requests",
                "valid-token"
            );

        when(
            jwtService.validateAndExtractClaims(
                "valid-token"
            )
        )
        .thenReturn(claims);

        when(
            jwtService.extractUserId(claims)
        )
        .thenReturn(USER_ID);

        when(
            jwtService.extractEmail(claims)
        )
        .thenReturn("user@example.com");

        when(
            jwtService.extractRoles(claims)
        )
        .thenReturn(List.of());

        AtomicReference<ServerWebExchange>
            forwardedExchange =
            new AtomicReference<>();

        StepVerifier.create(
            filter.filter(
                exchange,
                currentExchange -> {
                    forwardedExchange.set(
                        currentExchange
                    );

                    return Mono.empty();
                }
            )
        )
        .verifyComplete();

        assertThat(
            forwardedExchange
                .get()
                .getRequest()
                .getHeaders()
                .getFirst("X-User-Roles")
        )
        .isEmpty();
    }

    @Test
    void shouldExposeConfiguredFilterOrder() {
        assertThat(filter.getOrder())
            .isEqualTo(
                org.springframework.core.Ordered
                    .HIGHEST_PRECEDENCE
                    + 20
            );
    }

    private MockServerWebExchange exchangeFor(
        String path
    ) {
        return MockServerWebExchange.from(
            MockServerHttpRequest
                .get(path)
                .build()
        );
    }

    private MockServerWebExchange authenticatedExchange(
        String path,
        String token
    ) {
        return MockServerWebExchange.from(
            MockServerHttpRequest
                .get(path)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + token
                )
                .build()
        );
    }

    private String responseBody(
        MockServerWebExchange exchange
    ) {
        return exchange
            .getResponse()
            .getBodyAsString()
            .blockOptional()
            .orElse("");
    }
}
