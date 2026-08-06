package com.hlrms.apigateway.security;

import com.hlrms.apigateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String ISSUER =
        "hlrms-auth-service";

    private static final String SECRET =
        "hlrms-test-secret-key-that-is-long-enough-"
            + "for-hmac-sha-signing-123456789";

    private static final String OTHER_SECRET =
        "another-test-secret-key-that-is-long-enough-"
            + "for-hmac-sha-signing-987654321";

    private JwtProperties jwtProperties;

    private SecretKey secretKey;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {

        jwtProperties =
            new JwtProperties(
                ISSUER,
                SECRET
            );

        secretKey =
            Keys.hmacShaKeyFor(
                SECRET.getBytes(
                    StandardCharsets.UTF_8
                )
            );

        jwtService =
            new JwtService(jwtProperties);
    }

    @Test
    void shouldValidateValidToken() {

        String token =
            createToken(
                ISSUER,
                secretKey,
                Instant.now().minusSeconds(5),
                Instant.now().plusSeconds(300),
                List.of("USER")
            );

        assertThat(
            jwtService.isTokenValid(token)
        )
        .isTrue();
    }

    @Test
    void shouldExtractClaimsFromValidToken() {

        UUID userId =
            UUID.randomUUID();

        String token =
            createToken(
                userId.toString(),
                "user@example.com",
                ISSUER,
                secretKey,
                Instant.now().minusSeconds(5),
                Instant.now().plusSeconds(300),
                List.of("USER", "ADMIN")
            );

        Claims claims =
            jwtService.validateAndExtractClaims(
                token
            );

        assertThat(claims.getIssuer())
            .isEqualTo(ISSUER);

        assertThat(claims.getSubject())
            .isEqualTo(userId.toString());

        assertThat(
            claims.get("email", String.class)
        )
        .isEqualTo("user@example.com");
    }

    @Test
    void shouldExtractUserId() {

        UUID userId =
            UUID.randomUUID();

        Claims claims =
            parseClaims(
                createToken(
                    userId.toString(),
                    "user@example.com",
                    ISSUER,
                    secretKey,
                    Instant.now().minusSeconds(5),
                    Instant.now().plusSeconds(300),
                    List.of("USER")
                )
            );

        assertThat(
            jwtService.extractUserId(claims)
        )
        .isEqualTo(userId.toString());
    }

    @Test
    void shouldExtractEmail() {

        Claims claims =
            parseClaims(
                createToken(
                    UUID.randomUUID().toString(),
                    "user@example.com",
                    ISSUER,
                    secretKey,
                    Instant.now().minusSeconds(5),
                    Instant.now().plusSeconds(300),
                    List.of("USER")
                )
            );

        assertThat(
            jwtService.extractEmail(claims)
        )
        .isEqualTo("user@example.com");
    }

    @Test
    void shouldExtractRoles() {

        Claims claims =
            parseClaims(
                createToken(
                    UUID.randomUUID().toString(),
                    "admin@example.com",
                    ISSUER,
                    secretKey,
                    Instant.now().minusSeconds(5),
                    Instant.now().plusSeconds(300),
                    List.of(
                        "USER",
                        "ADMIN"
                    )
                )
            );

        assertThat(
            jwtService.extractRoles(claims)
        )
        .containsExactly(
            "USER",
            "ADMIN"
        );
    }

    @Test
    void shouldConvertRoleValuesToStrings() {

        Claims claims =
            Jwts.claims()
                .add(
                    Map.of(
                        "roles",
                        List.of(
                            "USER",
                            123,
                            true
                        )
                    )
                )
                .build();

        assertThat(
            jwtService.extractRoles(claims)
        )
        .containsExactly(
            "USER",
            "123",
            "true"
        );
    }

    @Test
    void shouldReturnEmptyRolesWhenClaimIsMissing() {

        Claims claims =
            Jwts.claims()
                .empty()
                .build();

        assertThat(
            jwtService.extractRoles(claims)
        )
        .isEmpty();
    }

    @Test
    void shouldReturnEmptyRolesWhenClaimIsNotAList() {

        Claims claims =
            Jwts.claims()
                .add(
                    "roles",
                    "USER"
                )
                .build();

        assertThat(
            jwtService.extractRoles(claims)
        )
        .isEmpty();
    }

    @Test
    void shouldRejectExpiredToken() {

        String expiredToken =
            createToken(
                ISSUER,
                secretKey,
                Instant.now().minusSeconds(600),
                Instant.now().minusSeconds(60),
                List.of("USER")
            );

        assertThat(
            jwtService.isTokenValid(
                expiredToken
            )
        )
        .isFalse();

        assertThatThrownBy(
            () ->
                jwtService
                    .validateAndExtractClaims(
                        expiredToken
                    )
        )
        .isInstanceOf(
            JwtException.class
        );
    }

    @Test
    void shouldRejectTokenWithWrongIssuer() {

        String token =
            createToken(
                "another-issuer",
                secretKey,
                Instant.now().minusSeconds(5),
                Instant.now().plusSeconds(300),
                List.of("USER")
            );

        assertThat(
            jwtService.isTokenValid(token)
        )
        .isFalse();

        assertThatThrownBy(
            () ->
                jwtService
                    .validateAndExtractClaims(
                        token
                    )
        )
        .isInstanceOf(
            JwtException.class
        );
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {

        SecretKey otherKey =
            Keys.hmacShaKeyFor(
                OTHER_SECRET.getBytes(
                    StandardCharsets.UTF_8
                )
            );

        String token =
            createToken(
                ISSUER,
                otherKey,
                Instant.now().minusSeconds(5),
                Instant.now().plusSeconds(300),
                List.of("USER")
            );

        assertThat(
            jwtService.isTokenValid(token)
        )
        .isFalse();

        assertThatThrownBy(
            () ->
                jwtService
                    .validateAndExtractClaims(
                        token
                    )
        )
        .isInstanceOf(
            JwtException.class
        );
    }

    @Test
    void shouldRejectTamperedToken() {

        String validToken =
            createToken(
                ISSUER,
                secretKey,
                Instant.now().minusSeconds(5),
                Instant.now().plusSeconds(300),
                List.of("USER")
            );

        String tamperedToken =
            validToken.substring(
                0,
                validToken.length() - 2
            )
            + "ab";

        assertThat(
            jwtService.isTokenValid(
                tamperedToken
            )
        )
        .isFalse();
    }

    @Test
    void shouldRejectMalformedToken() {

        assertThat(
            jwtService.isTokenValid(
                "this-is-not-a-jwt"
            )
        )
        .isFalse();

        assertThatThrownBy(
            () ->
                jwtService
                    .validateAndExtractClaims(
                        "this-is-not-a-jwt"
                    )
        )
        .isInstanceOf(
            JwtException.class
        );
    }

    @Test
    void shouldRejectBlankToken() {

        assertThat(
            jwtService.isTokenValid("")
        )
        .isFalse();

        assertThat(
            jwtService.isTokenValid("   ")
        )
        .isFalse();
    }

    @Test
    void shouldRejectNullToken() {

        assertThat(
            jwtService.isTokenValid(null)
        )
        .isFalse();
    }

    private Claims parseClaims(
        String token
    ) {
        return jwtService
            .validateAndExtractClaims(token);
    }

    private String createToken(
        String issuer,
        SecretKey signingKey,
        Instant issuedAt,
        Instant expiresAt,
        List<String> roles
    ) {
        return createToken(
            UUID.randomUUID().toString(),
            "user@example.com",
            issuer,
            signingKey,
            issuedAt,
            expiresAt,
            roles
        );
    }

    private String createToken(
        String userId,
        String email,
        String issuer,
        SecretKey signingKey,
        Instant issuedAt,
        Instant expiresAt,
        List<String> roles
    ) {
        return Jwts.builder()
            .issuer(issuer)
            .subject(userId)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .claim("email", email)
            .claim("roles", roles)
            .signWith(signingKey)
            .compact();
    }
}