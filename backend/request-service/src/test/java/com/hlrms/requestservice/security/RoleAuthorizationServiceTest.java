package com.hlrms.requestservice.security;

import com.hlrms.requestservice.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAuthorizationServiceTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    private RoleAuthorizationService service;

    @BeforeEach
    void setUp() {
        service =
            new RoleAuthorizationService(
                currentUserProvider
            );
    }

    @Test
    void shouldReturnCurrentUser() {

        CurrentUser currentUser =
            createUser(
                Set.of("USER")
            );

        when(
            currentUserProvider.getCurrentUser()
        )
        .thenReturn(currentUser);

        assertThat(service.currentUser())
            .isEqualTo(currentUser);

        verify(currentUserProvider)
            .getCurrentUser();
    }

    @Test
    void shouldIdentifyAdminRole() {

        when(
            currentUserProvider.getCurrentUser()
        )
        .thenReturn(
            createUser(
                Set.of("USER", "ADMIN")
            )
        );

        assertThat(service.isAdmin())
            .isTrue();
    }

    @Test
    void shouldReturnFalseWhenUserIsNotAdmin() {

        when(
            currentUserProvider.getCurrentUser()
        )
        .thenReturn(
            createUser(
                Set.of("USER")
            )
        );

        assertThat(service.isAdmin())
            .isFalse();
    }

    @Test
    void shouldIdentifyUserRole() {

        when(
            currentUserProvider.getCurrentUser()
        )
        .thenReturn(
            createUser(
                Set.of("USER")
            )
        );

        assertThat(service.isUser())
            .isTrue();
    }

    @Test
    void shouldReturnFalseWhenAdminDoesNotHaveUserRole() {

        when(
            currentUserProvider.getCurrentUser()
        )
        .thenReturn(
            createUser(
                Set.of("ADMIN")
            )
        );

        assertThat(service.isUser())
            .isFalse();
    }

    @Test
    void shouldAllowAdmin() {

        when(
            currentUserProvider.getCurrentUser()
        )
        .thenReturn(
            createUser(
                Set.of("ADMIN")
            )
        );

        service.requireAdmin();
    }

    @Test
    void shouldRejectNonAdmin() {

        when(
            currentUserProvider.getCurrentUser()
        )
        .thenReturn(
            createUser(
                Set.of("USER")
            )
        );

        assertThatThrownBy(
            () -> service.requireAdmin()
        )
        .isInstanceOf(
            ForbiddenException.class
        )
        .hasMessage(
            "Administrator privileges are required."
        );
    }

    @Test
    void shouldAllowUser() {

        when(
            currentUserProvider.getCurrentUser()
        )
        .thenReturn(
            createUser(
                Set.of("USER")
            )
        );

        service.requireUser();
    }

    @Test
    void shouldRejectAdminWithoutUserRole() {

        when(
            currentUserProvider.getCurrentUser()
        )
        .thenReturn(
            createUser(
                Set.of("ADMIN")
            )
        );

        assertThatThrownBy(
            () -> service.requireUser()
        )
        .isInstanceOf(
            ForbiddenException.class
        )
        .hasMessage(
            "User privileges are required."
        );
    }

    @Test
    void shouldAllowUserWithMultipleRoles() {

        when(
            currentUserProvider.getCurrentUser()
        )
        .thenReturn(
            createUser(
                Set.of(
                    "USER",
                    "ADMIN",
                    "AUDITOR"
                )
            )
        );

        service.requireUser();
        service.requireAdmin();
    }

    private CurrentUser createUser(
        Set<String> roles
    ) {
        return new CurrentUser(
            UUID.randomUUID(),
            "user@example.com",
            roles
        );
    }
}