package com.hlrms.transferapi.security;

import java.util.UUID;

public record CurrentUser(
        UUID id,
        String email
) {
}