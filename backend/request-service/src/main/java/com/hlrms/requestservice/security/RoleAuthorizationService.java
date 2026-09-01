package com.hlrms.requestservice.security;

import com.hlrms.requestservice.exception.ForbiddenException;
import org.springframework.stereotype.Service;

@Service
public class RoleAuthorizationService {

    private static final String USER_ROLE = "USER";
    private static final String ADMIN_ROLE = "ADMIN";

    private final CurrentUserProvider currentUserProvider;

    public RoleAuthorizationService(
            CurrentUserProvider currentUserProvider
    ) {
        this.currentUserProvider = currentUserProvider;
    }

    public CurrentUser currentUser() {
        return currentUserProvider.getCurrentUser();
    }

    public boolean isAdmin() {
        return currentUser().hasRole(ADMIN_ROLE);
    }

    public boolean isUser() {
        return currentUser().hasRole(USER_ROLE);
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new ForbiddenException(
                    "Administrator privileges are required."
            );
        }
    }

    public void requireUser() {
        if (!isUser()) {
            throw new ForbiddenException(
                    "User privileges are required."
            );
        }
    }
}