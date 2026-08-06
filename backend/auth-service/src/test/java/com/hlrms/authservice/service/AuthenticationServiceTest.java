package com.hlrms.authservice.service;

import com.hlrms.authservice.config.JwtProperties;
import com.hlrms.authservice.dto.request.LoginRequest;
import com.hlrms.authservice.dto.request.RefreshTokenRequest;
import com.hlrms.authservice.dto.request.RegisterRequest;
import com.hlrms.authservice.dto.response.AuthResponse;
import com.hlrms.authservice.dto.response.RegisterResponse;
import com.hlrms.authservice.entity.RefreshTokenEntity;
import com.hlrms.authservice.entity.RoleEntity;
import com.hlrms.authservice.entity.RoleName;
import com.hlrms.authservice.entity.UserEntity;
import com.hlrms.authservice.exception.EmailAlreadyExistsException;
import com.hlrms.authservice.exception.InvalidCredentialsException;
import com.hlrms.authservice.exception.InvalidRefreshTokenException;
import com.hlrms.authservice.metrics.AuthMetrics;
import com.hlrms.authservice.repository.RefreshTokenRepository;
import com.hlrms.authservice.repository.RoleRepository;
import com.hlrms.authservice.repository.UserRepository;
import com.hlrms.authservice.security.JwtService;
import com.hlrms.authservice.security.TokenHashService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenHashService tokenHashService;

    @Mock
    private AuthMetrics authMetrics;

    private JwtProperties jwtProperties;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        jwtProperties =
            new JwtProperties(
                "hlrms-auth-service",
                "a".repeat(64),
                Duration.ofMinutes(15),
                Duration.ofDays(7)
            );

        service =
            new AuthenticationService(
                userRepository,
                roleRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                tokenHashService,
                jwtProperties,
                authMetrics
            );
    }

    @Test
    void shouldRegisterUserSuccessfully() {

        RegisterRequest request =
            new RegisterRequest(
                "  User@Example.COM  ",
                "StrongPassword123",
                "  John  ",
                "  Doe  "
            );

        RoleEntity userRole =
            new RoleEntity(RoleName.USER);

        when(
            userRepository.existsByEmail(
                "user@example.com"
            )
        )
        .thenReturn(false);

        when(
            roleRepository.findByName(
                RoleName.USER
            )
        )
        .thenReturn(Optional.of(userRole));

        when(
            passwordEncoder.encode(
                "StrongPassword123"
            )
        )
        .thenReturn("encoded-password");

        when(
            userRepository.save(
                any(UserEntity.class)
            )
        )
        .thenAnswer(invocation -> {
            UserEntity user =
                invocation.getArgument(0);

            user.setId(UUID.randomUUID());
            user.setCreatedAt(Instant.now());

            return user;
        });

        RegisterResponse response =
            service.register(request);

        ArgumentCaptor<UserEntity> userCaptor =
            ArgumentCaptor.forClass(
                UserEntity.class
            );

        verify(userRepository)
            .save(userCaptor.capture());

        UserEntity savedUser =
            userCaptor.getValue();

        assertThat(savedUser.getEmail())
            .isEqualTo("user@example.com");

        assertThat(savedUser.getPasswordHash())
            .isEqualTo("encoded-password");

        assertThat(savedUser.getFirstName())
            .isEqualTo("John");

        assertThat(savedUser.getLastName())
            .isEqualTo("Doe");

        assertThat(savedUser.getRoles())
            .containsExactly(userRole);

        assertThat(response.id())
            .isNotNull();

        assertThat(response.email())
            .isEqualTo("user@example.com");

        assertThat(response.firstName())
            .isEqualTo("John");

        assertThat(response.lastName())
            .isEqualTo("Doe");

        assertThat(response.createdAt())
            .isNotNull();

        verify(authMetrics)
            .registerSuccess();
    }

    @Test
    void shouldRejectDuplicateEmail() {

        RegisterRequest request =
            new RegisterRequest(
                "User@Example.com",
                "StrongPassword123",
                "John",
                "Doe"
            );

        when(
            userRepository.existsByEmail(
                "user@example.com"
            )
        )
        .thenReturn(true);

        assertThatThrownBy(
            () -> service.register(request)
        )
        .isInstanceOf(
            EmailAlreadyExistsException.class
        )
        .hasMessage(
            "Email already exists: user@example.com"
        );

        verify(
            roleRepository,
            never()
        )
        .findByName(any());

        verify(
            passwordEncoder,
            never()
        )
        .encode(any());

        verify(
            userRepository,
            never()
        )
        .save(any());

        verify(
            authMetrics,
            never()
        )
        .registerSuccess();
    }

    @Test
    void shouldFailRegistrationWhenUserRoleIsMissing() {

        RegisterRequest request =
            new RegisterRequest(
                "user@example.com",
                "StrongPassword123",
                "John",
                "Doe"
            );

        when(
            userRepository.existsByEmail(
                "user@example.com"
            )
        )
        .thenReturn(false);

        when(
            roleRepository.findByName(
                RoleName.USER
            )
        )
        .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> service.register(request)
        )
        .isInstanceOf(
            EntityNotFoundException.class
        )
        .hasMessage("Role USER not found");

        verify(
            userRepository,
            never()
        )
        .save(any());

        verify(
            authMetrics,
            never()
        )
        .registerSuccess();
    }

    @Test
    void shouldLoginSuccessfully() {

        UserEntity user =
            createUser();

        when(
            userRepository.findByEmail(
                "user@example.com"
            )
        )
        .thenReturn(Optional.of(user));

        when(
            passwordEncoder.matches(
                "StrongPassword123",
                "encoded-password"
            )
        )
        .thenReturn(true);

        when(
            jwtService.generateAccessToken(user)
        )
        .thenReturn("access-token");

        when(
            jwtService.generateRefreshToken()
        )
        .thenReturn("refresh-token");

        when(
            tokenHashService.hash(
                "refresh-token"
            )
        )
        .thenReturn("refresh-token-hash");

        AuthResponse response =
            service.login(
                new LoginRequest(
                    " User@Example.COM ",
                    "StrongPassword123"
                )
            );

        assertThat(response.accessToken())
            .isEqualTo("access-token");

        assertThat(response.refreshToken())
            .isEqualTo("refresh-token");

        assertThat(response.tokenType())
            .isEqualTo("Bearer");

        ArgumentCaptor<RefreshTokenEntity>
            tokenCaptor =
            ArgumentCaptor.forClass(
                RefreshTokenEntity.class
            );

        verify(refreshTokenRepository)
            .save(tokenCaptor.capture());

        RefreshTokenEntity savedToken =
            tokenCaptor.getValue();

        assertThat(savedToken.getUser())
            .isEqualTo(user);

        assertThat(savedToken.getTokenHash())
            .isEqualTo("refresh-token-hash");

        assertThat(savedToken.getExpiresAt())
            .isAfter(Instant.now());

        assertThat(savedToken.getExpiresAt())
            .isBefore(
                Instant.now().plus(
                    Duration.ofDays(8)
                )
            );

        verify(authMetrics)
            .loginSuccess();

        verify(
            authMetrics,
            never()
        )
        .loginFailed();
    }

    @Test
    void shouldRejectLoginWhenEmailDoesNotExist() {

        when(
            userRepository.findByEmail(
                "unknown@example.com"
            )
        )
        .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> service.login(
                new LoginRequest(
                    "unknown@example.com",
                    "StrongPassword123"
                )
            )
        )
        .isInstanceOf(
            InvalidCredentialsException.class
        )
        .hasMessage(
            "Invalid email or password"
        );

        verify(authMetrics)
            .loginFailed();

        verify(
            passwordEncoder,
            never()
        )
        .matches(any(), any());

        verify(
            jwtService,
            never()
        )
        .generateAccessToken(any());
    }

    @Test
    void shouldRejectLoginWithWrongPassword() {

        UserEntity user =
            createUser();

        when(
            userRepository.findByEmail(
                "user@example.com"
            )
        )
        .thenReturn(Optional.of(user));

        when(
            passwordEncoder.matches(
                "wrong-password",
                "encoded-password"
            )
        )
        .thenReturn(false);

        assertThatThrownBy(
            () -> service.login(
                new LoginRequest(
                    "user@example.com",
                    "wrong-password"
                )
            )
        )
        .isInstanceOf(
            InvalidCredentialsException.class
        );

        verify(authMetrics)
            .loginFailed();

        verify(
            authMetrics,
            never()
        )
        .loginSuccess();

        verify(
            jwtService,
            never()
        )
        .generateAccessToken(any());
    }

    @Test
    void shouldRejectDisabledUserLogin() {

        UserEntity user =
            createUser();

        user.setEnabled(false);

        when(
            userRepository.findByEmail(
                "user@example.com"
            )
        )
        .thenReturn(Optional.of(user));

        when(
            passwordEncoder.matches(
                "StrongPassword123",
                "encoded-password"
            )
        )
        .thenReturn(true);

        assertThatThrownBy(
            () -> service.login(
                new LoginRequest(
                    "user@example.com",
                    "StrongPassword123"
                )
            )
        )
        .isInstanceOf(
            InvalidCredentialsException.class
        );

        verify(
            jwtService,
            never()
        )
        .generateAccessToken(any());
    }

    @Test
    void shouldRejectLockedUserLogin() {

        UserEntity user =
            createUser();

        user.setAccountLocked(true);

        when(
            userRepository.findByEmail(
                "user@example.com"
            )
        )
        .thenReturn(Optional.of(user));

        when(
            passwordEncoder.matches(
                "StrongPassword123",
                "encoded-password"
            )
        )
        .thenReturn(true);

        assertThatThrownBy(
            () -> service.login(
                new LoginRequest(
                    "user@example.com",
                    "StrongPassword123"
                )
            )
        )
        .isInstanceOf(
            InvalidCredentialsException.class
        );

        verify(
            jwtService,
            never()
        )
        .generateAccessToken(any());
    }

    @Test
    void shouldRotateRefreshTokenSuccessfully() {

        UserEntity user =
            createUser();

        RefreshTokenEntity currentToken =
            new RefreshTokenEntity(
                user,
                "old-token-hash",
                Instant.now().plusSeconds(3600)
            );

        when(
            tokenHashService.hash(
                "old-refresh-token"
            )
        )
        .thenReturn("old-token-hash");

        when(
            refreshTokenRepository
                .findByTokenHash(
                    "old-token-hash"
                )
        )
        .thenReturn(
            Optional.of(currentToken)
        );

        when(
            jwtService.generateAccessToken(user)
        )
        .thenReturn("new-access-token");

        when(
            jwtService.generateRefreshToken()
        )
        .thenReturn("new-refresh-token");

        when(
            tokenHashService.hash(
                "new-refresh-token"
            )
        )
        .thenReturn("new-token-hash");

        AuthResponse response =
            service.refresh(
                new RefreshTokenRequest(
                    "old-refresh-token"
                )
            );

        assertThat(currentToken.isRevoked())
            .isTrue();

        ArgumentCaptor<RefreshTokenEntity>
            tokenCaptor =
            ArgumentCaptor.forClass(
                RefreshTokenEntity.class
            );

        verify(
            refreshTokenRepository,
            org.mockito.Mockito.times(2)
        )
        .save(tokenCaptor.capture());

        assertThat(tokenCaptor.getAllValues())
            .hasSize(2);

        RefreshTokenEntity revokedOldToken =
            tokenCaptor.getAllValues().get(0);

        RefreshTokenEntity newToken =
            tokenCaptor.getAllValues().get(1);

        assertThat(revokedOldToken)
            .isSameAs(currentToken);

        assertThat(revokedOldToken.isRevoked())
            .isTrue();

        assertThat(newToken.getTokenHash())
            .isEqualTo("new-token-hash");

        assertThat(newToken.getUser())
            .isEqualTo(user);

        assertThat(newToken.isRevoked())
            .isFalse();

        assertThat(newToken.getExpiresAt())
            .isAfter(Instant.now());

        assertThat(response.accessToken())
            .isEqualTo("new-access-token");

        assertThat(response.refreshToken())
            .isEqualTo("new-refresh-token");

        verify(authMetrics)
            .refreshSuccess();
    }

    @Test
    void shouldRejectUnknownRefreshToken() {

        when(
            tokenHashService.hash(
                "unknown-token"
            )
        )
        .thenReturn("unknown-hash");

        when(
            refreshTokenRepository
                .findByTokenHash(
                    "unknown-hash"
                )
        )
        .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> service.refresh(
                new RefreshTokenRequest(
                    "unknown-token"
                )
            )
        )
        .isInstanceOf(
            InvalidRefreshTokenException.class
        )
        .hasMessage(
            "Invalid or expired refresh token"
        );

        verify(
            refreshTokenRepository,
            never()
        )
        .save(any());

        verify(
            authMetrics,
            never()
        )
        .refreshSuccess();
    }

    @Test
    void shouldRejectExpiredRefreshToken() {

        UserEntity user =
            createUser();

        RefreshTokenEntity expiredToken =
            new RefreshTokenEntity(
                user,
                "expired-hash",
                Instant.now().minusSeconds(1)
            );

        when(
            tokenHashService.hash(
                "expired-token"
            )
        )
        .thenReturn("expired-hash");

        when(
            refreshTokenRepository
                .findByTokenHash(
                    "expired-hash"
                )
        )
        .thenReturn(
            Optional.of(expiredToken)
        );

        assertThatThrownBy(
            () -> service.refresh(
                new RefreshTokenRequest(
                    "expired-token"
                )
            )
        )
        .isInstanceOf(
            InvalidRefreshTokenException.class
        );

        verify(
            refreshTokenRepository,
            never()
        )
        .save(any());

        verify(
            jwtService,
            never()
        )
        .generateAccessToken(any());
    }

    @Test
    void shouldRejectRevokedRefreshToken() {

        UserEntity user =
            createUser();

        RefreshTokenEntity revokedToken =
            new RefreshTokenEntity(
                user,
                "revoked-hash",
                Instant.now().plusSeconds(3600)
            );

        revokedToken.revoke();

        when(
            tokenHashService.hash(
                "revoked-token"
            )
        )
        .thenReturn("revoked-hash");

        when(
            refreshTokenRepository
                .findByTokenHash(
                    "revoked-hash"
                )
        )
        .thenReturn(
            Optional.of(revokedToken)
        );

        assertThatThrownBy(
            () -> service.refresh(
                new RefreshTokenRequest(
                    "revoked-token"
                )
            )
        )
        .isInstanceOf(
            InvalidRefreshTokenException.class
        );

        verify(
            refreshTokenRepository,
            never()
        )
        .save(any());

        verify(
            authMetrics,
            never()
        )
        .refreshSuccess();
    }

    @Test
    void shouldRejectRefreshForDisabledUser() {

        UserEntity user =
            createUser();

        user.setEnabled(false);

        RefreshTokenEntity activeToken =
            new RefreshTokenEntity(
                user,
                "active-hash",
                Instant.now().plusSeconds(3600)
            );

        when(
            tokenHashService.hash(
                "active-token"
            )
        )
        .thenReturn("active-hash");

        when(
            refreshTokenRepository
                .findByTokenHash(
                    "active-hash"
                )
        )
        .thenReturn(
            Optional.of(activeToken)
        );

        assertThatThrownBy(
            () -> service.refresh(
                new RefreshTokenRequest(
                    "active-token"
                )
            )
        )
        .isInstanceOf(
            InvalidCredentialsException.class
        );

        assertThat(activeToken.isRevoked())
            .isFalse();

        verify(
            refreshTokenRepository,
            never()
        )
        .save(any());
    }

    private UserEntity createUser() {
        UserEntity user =
            new UserEntity(
                "user@example.com",
                "encoded-password",
                "John",
                "Doe"
            );

        user.setId(UUID.randomUUID());
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.setEnabled(true);
        user.setAccountLocked(false);

        user.addRole(
            new RoleEntity(RoleName.USER)
        );

        return user;
    }
}