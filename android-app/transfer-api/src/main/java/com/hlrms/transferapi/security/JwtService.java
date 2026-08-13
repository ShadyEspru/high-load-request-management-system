package com.hlrms.transferapi.security;

import com.hlrms.transferapi.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(
            JwtProperties properties
    ) {
        this.properties = properties;

        this.key = Keys.hmacShaKeyFor(
            properties.secret()
                .getBytes(StandardCharsets.UTF_8)
        );
    }

    public Claims parse(String token) {

        return Jwts.parser()
            .verifyWith(key)
            .requireIssuer(properties.issuer())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public UUID extractUserId(
            Claims claims
    ) {
        return UUID.fromString(
            claims.getSubject()
        );
    }

    public String extractEmail(
            Claims claims
    ) {
        return claims.get(
            "email",
            String.class
        );
    }
}