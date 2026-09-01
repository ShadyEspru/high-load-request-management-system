package com.hlrms.requestservice.security;

import java.util.Set;
import java.util.UUID;

public record CurrentUser(
    UUID userId,
    String email,
    Set<String> roles
) {

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}