package com.hlrms.authservice.security;

import com.hlrms.authservice.config.JwtProperties;
import com.hlrms.authservice.entity.RoleEntity;
import com.hlrms.authservice.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
//import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.secret()
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(UserEntity user) {

        Instant now = Instant.now();

        Instant expiration =
                now.plus(jwtProperties.accessTokenExpiration());

        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim(
                        "email",
                        user.getEmail()
                )
                .claim(
                        "roles",
                        user.getRoles()
                                .stream()
                                .map(RoleEntity::getName)
                                .map(Enum::name)
                                .collect(Collectors.toSet())
                )
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken() {

        return java.util.UUID.randomUUID().toString()
                + "-"
                + java.util.UUID.randomUUID();
    }

    public Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {

        try {

            extractClaims(token);

            return true;

        } catch (Exception ignored) {

            return false;

        }
    }

}