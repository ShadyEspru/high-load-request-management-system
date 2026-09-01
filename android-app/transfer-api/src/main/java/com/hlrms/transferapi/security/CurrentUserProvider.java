package com.hlrms.transferapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUserProvider {

    private final JwtService jwtService;

    public CurrentUserProvider(
            JwtService jwtService
    ) {
        this.jwtService = jwtService;
    }

    public CurrentUser fromAuthorizationHeader(
            String authorization
    ) {

        if (
                authorization == null ||
                        !authorization.startsWith("Bearer ")
        ) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Missing Bearer token"
            );
        }

        String token =
                authorization
                        .substring(7)
                        .trim();

        if (token.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Missing Bearer token"
            );
        }

        try {

            Claims claims =
                    jwtService.parse(token);

            return new CurrentUser(
                    jwtService.extractUserId(claims),
                    jwtService.extractEmail(claims)
            );

        } catch (
                JwtException |
                IllegalArgumentException exception
        ) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired access token",
                    exception
            );
        }
    }
}