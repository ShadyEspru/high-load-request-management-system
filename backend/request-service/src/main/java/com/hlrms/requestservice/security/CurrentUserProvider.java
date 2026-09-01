package com.hlrms.requestservice.security;

import com.hlrms.requestservice.exception.TrustedUserHeaderException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private static final String USER_ID_HEADER =
        "X-User-Id";

    private static final String USER_EMAIL_HEADER =
        "X-User-Email";

    private static final String USER_ROLES_HEADER =
        "X-User-Roles";

    private final HttpServletRequest httpServletRequest;

    public CurrentUser getCurrentUser() {
        String userIdHeader =
            requireHeader(USER_ID_HEADER);

        String email =
            requireHeader(USER_EMAIL_HEADER);

        String rolesHeader =
            requireHeader(USER_ROLES_HEADER);

        UUID userId = parseUserId(userIdHeader);

        Set<String> roles =
            parseRoles(rolesHeader);

        return new CurrentUser(
            userId,
            email,
            roles
        );
    }

    public UUID getUserId() {
        return getCurrentUser().userId();
    }

    public String getEmail() {
        return getCurrentUser().email();
    }

    public Set<String> getRoles() {
        return getCurrentUser().roles();
    }

    private String requireHeader(
        String headerName
    ) {
        String value =
            httpServletRequest.getHeader(headerName);

        if (value == null || value.isBlank()) {
            throw new TrustedUserHeaderException(
                "Required trusted identity header is missing: "
                    + headerName
            );
        }

        return value.trim();
    }

    private UUID parseUserId(
        String userIdHeader
    ) {
        try {
            return UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException exception) {
            throw new TrustedUserHeaderException(
                "Trusted identity header X-User-Id "
                    + "does not contain a valid UUID",
                exception
            );
        }
    }

    private Set<String> parseRoles(
        String rolesHeader
    ) {
        Set<String> roles =
            Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .collect(
                    Collectors.toCollection(
                        LinkedHashSet::new
                    )
                );

        if (roles.isEmpty()) {
            throw new TrustedUserHeaderException(
                "Trusted identity header X-User-Roles "
                    + "does not contain any roles"
            );
        }

        return Set.copyOf(roles);
    }
}