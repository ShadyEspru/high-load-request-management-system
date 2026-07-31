package com.hlrms.authservice.dto.response;

public record AuthResponse(

        String accessToken,

        String refreshToken,

        String tokenType

) {
}