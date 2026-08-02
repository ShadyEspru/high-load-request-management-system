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
import com.hlrms.authservice.repository.RefreshTokenRepository;
import com.hlrms.authservice.repository.RoleRepository;
import com.hlrms.authservice.repository.UserRepository;
import com.hlrms.authservice.security.JwtService;
import com.hlrms.authservice.security.TokenHashService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.hlrms.authservice.metrics.AuthMetrics;

import java.time.Instant;
import java.util.Locale;

@Service
@Transactional
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenHashService tokenHashService;
    private final JwtProperties jwtProperties;
    private final AuthMetrics authMetrics;

    public AuthenticationService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenHashService tokenHashService,
            JwtProperties jwtProperties,
            AuthMetrics authMetrics
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenHashService = tokenHashService;
        this.jwtProperties = jwtProperties;
        this.authMetrics = authMetrics;
    }

    public RegisterResponse register(RegisterRequest request) {

        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        RoleEntity userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() ->
                        new EntityNotFoundException("Role USER not found")
                );

        UserEntity user = new UserEntity(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim()
        );

        user.addRole(userRole);

        UserEntity savedUser = userRepository.save(user);

        authMetrics.registerSuccess();

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getCreatedAt()
        );
    }

    public AuthResponse login(LoginRequest request) {

        String normalizedEmail = normalizeEmail(request.email());

        UserEntity user;

        try {
            user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(InvalidCredentialsException::new);
        } catch (InvalidCredentialsException e) {
            authMetrics.loginFailed();
            throw e;
        }

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            authMetrics.loginFailed();
            throw new InvalidCredentialsException();
        }

        validateUserAccount(user);

        authMetrics.loginSuccess();

        return createAuthenticationResponse(user);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {

        String currentTokenHash =
                tokenHashService.hash(request.refreshToken());

        RefreshTokenEntity currentRefreshToken =
                refreshTokenRepository.findByTokenHash(currentTokenHash)
                        .orElseThrow(InvalidRefreshTokenException::new);

        if (!currentRefreshToken.isActive()) {
            throw new InvalidRefreshTokenException();
        }

        UserEntity user = currentRefreshToken.getUser();

        validateUserAccount(user);

        currentRefreshToken.revoke();

        refreshTokenRepository.save(currentRefreshToken);

        authMetrics.refreshSuccess();

        return createAuthenticationResponse(user);
    }

    private AuthResponse createAuthenticationResponse(UserEntity user) {

        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                jwtService.generateRefreshToken();

        String refreshTokenHash =
                tokenHashService.hash(refreshToken);

        Instant refreshTokenExpiration =
                Instant.now().plus(
                        jwtProperties.refreshTokenExpiration()
                );

        RefreshTokenEntity refreshTokenEntity =
                new RefreshTokenEntity(
                        user,
                        refreshTokenHash,
                        refreshTokenExpiration
                );

        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer"
        );
    }

    private void validateUserAccount(UserEntity user) {

        if (!user.isEnabled() || user.isAccountLocked()) {
            throw new InvalidCredentialsException();
        }
    }

    private String normalizeEmail(String email) {

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}