package com.hlrms.requestservice.security;

import com.hlrms.requestservice.exception.TrustedUserHeaderException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserProviderTest {

    @Mock
    private HttpServletRequest httpServletRequest;

    private CurrentUserProvider currentUserProvider;

    @BeforeEach
    void setUp() {
        currentUserProvider =
            new CurrentUserProvider(
                httpServletRequest
            );
    }

    @Test
    void shouldBuildCurrentUserFromTrustedHeaders() {

        UUID userId = UUID.randomUUID();

        when(
            httpServletRequest.getHeader(
                "X-User-Id"
            )
        )
        .thenReturn(userId.toString());

        when(
            httpServletRequest.getHeader(
                "X-User-Email"
            )
        )
        .thenReturn("user@example.com");

        when(
            httpServletRequest.getHeader(
                "X-User-Roles"
            )
        )
        .thenReturn("USER,ADMIN");

        CurrentUser currentUser =
            currentUserProvider.getCurrentUser();

        assertThat(currentUser.userId())
            .isEqualTo(userId);

        assertThat(currentUser.email())
            .isEqualTo("user@example.com");

        assertThat(currentUser.roles())
            .containsExactlyInAnyOrder(
                "USER",
                "ADMIN"
            );

        assertThat(
            currentUser.hasRole("USER")
        )
        .isTrue();

        assertThat(
            currentUser.hasRole("ADMIN")
        )
        .isTrue();
    }

    @Test
    void shouldTrimTrustedHeaderValues() {

        UUID userId = UUID.randomUUID();

        when(
            httpServletRequest.getHeader(
                "X-User-Id"
            )
        )
        .thenReturn(
            "  " + userId + "  "
        );

        when(
            httpServletRequest.getHeader(
                "X-User-Email"
            )
        )
        .thenReturn(
            "  user@example.com  "
        );

        when(
            httpServletRequest.getHeader(
                "X-User-Roles"
            )
        )
        .thenReturn(
            " USER , ADMIN "
        );

        CurrentUser currentUser =
            currentUserProvider.getCurrentUser();

        assertThat(currentUser.userId())
            .isEqualTo(userId);

        assertThat(currentUser.email())
            .isEqualTo("user@example.com");

        assertThat(currentUser.roles())
            .containsExactlyInAnyOrder(
                "USER",
                "ADMIN"
            );
    }

    @Test
    void shouldRemoveBlankRolesAndDuplicates() {

        UUID userId = UUID.randomUUID();

        when(
            httpServletRequest.getHeader(
                "X-User-Id"
            )
        )
        .thenReturn(userId.toString());

        when(
            httpServletRequest.getHeader(
                "X-User-Email"
            )
        )
        .thenReturn("user@example.com");

        when(
            httpServletRequest.getHeader(
                "X-User-Roles"
            )
        )
        .thenReturn(
            "USER, ,ADMIN,USER,"
        );

        Set<String> roles =
            currentUserProvider.getRoles();

        assertThat(roles)
            .containsExactlyInAnyOrder(
                "USER",
                "ADMIN"
            );
    }

    @Test
    void shouldReturnUserIdDirectly() {

        UUID userId = UUID.randomUUID();

        prepareValidHeaders(
            userId,
            "user@example.com",
            "USER"
        );

        assertThat(
            currentUserProvider.getUserId()
        )
        .isEqualTo(userId);
    }

    @Test
    void shouldReturnEmailDirectly() {

        UUID userId = UUID.randomUUID();

        prepareValidHeaders(
            userId,
            "user@example.com",
            "USER"
        );

        assertThat(
            currentUserProvider.getEmail()
        )
        .isEqualTo("user@example.com");
    }

    @Test
    void shouldReturnRolesDirectly() {

        UUID userId = UUID.randomUUID();

        prepareValidHeaders(
            userId,
            "user@example.com",
            "USER,ADMIN"
        );

        assertThat(
            currentUserProvider.getRoles()
        )
        .containsExactlyInAnyOrder(
            "USER",
            "ADMIN"
        );
    }

    @Test
    void shouldRejectMissingUserIdHeader() {

        when(
            httpServletRequest.getHeader(
                "X-User-Id"
            )
        )
        .thenReturn(null);

        assertThatThrownBy(
            () -> currentUserProvider
                .getCurrentUser()
        )
        .isInstanceOf(
            TrustedUserHeaderException.class
        )
        .hasMessage(
            "Required trusted identity header "
                + "is missing: X-User-Id"
        );
    }

    @Test
    void shouldRejectBlankUserIdHeader() {

        when(
            httpServletRequest.getHeader(
                "X-User-Id"
            )
        )
        .thenReturn("   ");

        assertThatThrownBy(
            () -> currentUserProvider
                .getCurrentUser()
        )
        .isInstanceOf(
            TrustedUserHeaderException.class
        )
        .hasMessageContaining(
            "X-User-Id"
        );
    }

    @Test
    void shouldRejectInvalidUserId() {

        when(
            httpServletRequest.getHeader(
                "X-User-Id"
            )
        )
        .thenReturn("not-a-uuid");

        when(
            httpServletRequest.getHeader(
                "X-User-Email"
            )
        )
        .thenReturn("user@example.com");

        when(
            httpServletRequest.getHeader(
                "X-User-Roles"
            )
        )
        .thenReturn("USER");

        assertThatThrownBy(
            () -> currentUserProvider
                .getCurrentUser()
        )
        .isInstanceOf(
            TrustedUserHeaderException.class
        )
        .hasMessage(
            "Trusted identity header X-User-Id "
                + "does not contain a valid UUID"
        )
        .hasCauseInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    void shouldRejectMissingEmailHeader() {

        UUID userId = UUID.randomUUID();

        when(
            httpServletRequest.getHeader(
                "X-User-Id"
            )
        )
        .thenReturn(userId.toString());

        when(
            httpServletRequest.getHeader(
                "X-User-Email"
            )
        )
        .thenReturn(null);

        assertThatThrownBy(
            () -> currentUserProvider
                .getCurrentUser()
        )
        .isInstanceOf(
            TrustedUserHeaderException.class
        )
        .hasMessage(
            "Required trusted identity header "
                + "is missing: X-User-Email"
        );
    }

    @Test
    void shouldRejectMissingRolesHeader() {

        UUID userId = UUID.randomUUID();

        when(
            httpServletRequest.getHeader(
                "X-User-Id"
            )
        )
        .thenReturn(userId.toString());

        when(
            httpServletRequest.getHeader(
                "X-User-Email"
            )
        )
        .thenReturn("user@example.com");

        when(
            httpServletRequest.getHeader(
                "X-User-Roles"
            )
        )
        .thenReturn(null);

        assertThatThrownBy(
            () -> currentUserProvider
                .getCurrentUser()
        )
        .isInstanceOf(
            TrustedUserHeaderException.class
        )
        .hasMessage(
            "Required trusted identity header "
                + "is missing: X-User-Roles"
        );
    }

    @Test
    void shouldRejectRolesHeaderWithoutRoles() {

        UUID userId = UUID.randomUUID();

        when(
            httpServletRequest.getHeader(
                "X-User-Id"
            )
        )
        .thenReturn(userId.toString());

        when(
            httpServletRequest.getHeader(
                "X-User-Email"
            )
        )
        .thenReturn("user@example.com");

        when(
            httpServletRequest.getHeader(
                "X-User-Roles"
            )
        )
        .thenReturn(" , , ");

        assertThatThrownBy(
            () -> currentUserProvider
                .getCurrentUser()
        )
        .isInstanceOf(
            TrustedUserHeaderException.class
        )
        .hasMessage(
            "Trusted identity header X-User-Roles "
                + "does not contain any roles"
        );
    }

    private void prepareValidHeaders(
        UUID userId,
        String email,
        String roles
    ) {
        when(
            httpServletRequest.getHeader(
                "X-User-Id"
            )
        )
        .thenReturn(userId.toString());

        when(
            httpServletRequest.getHeader(
                "X-User-Email"
            )
        )
        .thenReturn(email);

        when(
            httpServletRequest.getHeader(
                "X-User-Roles"
            )
        )
        .thenReturn(roles);
    }
}