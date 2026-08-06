package com.hlrms.authservice.controller;

import com.hlrms.authservice.dto.request.LoginRequest;
import com.hlrms.authservice.dto.request.RefreshTokenRequest;
import com.hlrms.authservice.dto.request.RegisterRequest;
import com.hlrms.authservice.dto.response.AuthResponse;
import com.hlrms.authservice.dto.response.RegisterResponse;
import com.hlrms.authservice.exception.EmailAlreadyExistsException;
import com.hlrms.authservice.exception.InvalidCredentialsException;
import com.hlrms.authservice.exception.InvalidRefreshTokenException;
import com.hlrms.authservice.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    private static final String BASE_URL =
        "/api/v1/auth";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService
        authenticationService;

    @Test
    void shouldRegisterUserAndReturn201()
        throws Exception {

        UUID userId =
            UUID.randomUUID();

        Instant createdAt =
            Instant.now();

        when(
            authenticationService.register(
                any(RegisterRequest.class)
            )
        )
        .thenReturn(
            new RegisterResponse(
                userId,
                "user@example.com",
                "John",
                "Doe",
                createdAt
            )
        );

        mockMvc.perform(
            post(BASE_URL + "/register")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content(
                    """
                    {
                      "email": "user@example.com",
                      "password": "StrongPassword123",
                      "firstName": "John",
                      "lastName": "Doe"
                    }
                    """
                )
        )
        .andExpect(status().isCreated())
        .andExpect(
            content()
                .contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON
                )
        )
        .andExpect(
            jsonPath("$.id")
                .value(userId.toString())
        )
        .andExpect(
            jsonPath("$.email")
                .value("user@example.com")
        )
        .andExpect(
            jsonPath("$.firstName")
                .value("John")
        )
        .andExpect(
            jsonPath("$.lastName")
                .value("Doe")
        )
        .andExpect(
            jsonPath("$.createdAt")
                .exists()
        );

        verify(authenticationService)
            .register(
                any(RegisterRequest.class)
            );
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists()
        throws Exception {

        when(
            authenticationService.register(
                any(RegisterRequest.class)
            )
        )
        .thenThrow(
            new EmailAlreadyExistsException(
                "user@example.com"
            )
        );

        mockMvc.perform(
            post(BASE_URL + "/register")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content(
                    """
                    {
                      "email": "user@example.com",
                      "password": "StrongPassword123",
                      "firstName": "John",
                      "lastName": "Doe"
                    }
                    """
                )
        )
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.status")
                .value(409)
        )
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Email already exists: "
                        + "user@example.com"
                )
        )
        .andExpect(
            jsonPath("$.path")
                .value(
                    "/api/v1/auth/register"
                )
        );
    }

    @Test
    void shouldRejectInvalidRegistrationData()
        throws Exception {

        mockMvc.perform(
            post(BASE_URL + "/register")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content(
                    """
                    {
                      "email": "not-an-email",
                      "password": "short",
                      "firstName": "",
                      "lastName": ""
                    }
                    """
                )
        )
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.status")
                .value(400)
        )
        .andExpect(
            jsonPath("$.message")
                .value("Validation failed")
        )
        .andExpect(
            jsonPath("$.validationErrors")
                .isArray()
        )
        .andExpect(
            jsonPath(
                "$.validationErrors.length()"
            )
            .value(4)
        );

        verify(
            authenticationService,
            never()
        )
        .register(any());
    }

    @Test
    void shouldRejectBlankRegistrationBody()
        throws Exception {

        mockMvc.perform(
            post(BASE_URL + "/register")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content(
                    """
                    {
                      "email": "",
                      "password": "",
                      "firstName": "",
                      "lastName": ""
                    }
                    """
                )
        )
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Validation failed")
        )
        .andExpect(
            jsonPath("$.validationErrors")
                .isArray()
        );
    }

    @Test
    void shouldLoginAndReturnTokens()
        throws Exception {

        when(
            authenticationService.login(
                any(LoginRequest.class)
            )
        )
        .thenReturn(
            new AuthResponse(
                "access-token-value",
                "refresh-token-value",
                "Bearer"
            )
        );

        mockMvc.perform(
            post(BASE_URL + "/login")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content(
                    """
                    {
                      "email": "user@example.com",
                      "password": "StrongPassword123"
                    }
                    """
                )
        )
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.accessToken")
                .value("access-token-value")
        )
        .andExpect(
            jsonPath("$.refreshToken")
                .value("refresh-token-value")
        )
        .andExpect(
            jsonPath("$.tokenType")
                .value("Bearer")
        );

        verify(authenticationService)
            .login(any(LoginRequest.class));
    }

    @Test
    void shouldReturn401ForInvalidCredentials()
        throws Exception {

        when(
            authenticationService.login(
                any(LoginRequest.class)
            )
        )
        .thenThrow(
            new InvalidCredentialsException()
        );

        mockMvc.perform(
            post(BASE_URL + "/login")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content(
                    """
                    {
                      "email": "user@example.com",
                      "password": "wrong-password"
                    }
                    """
                )
        )
        .andExpect(status().isUnauthorized())
        .andExpect(
            jsonPath("$.status")
                .value(401)
        )
        .andExpect(
            jsonPath("$.message")
                .value("Invalid email or password")
        )
        .andExpect(
            jsonPath("$.path")
                .value("/api/v1/auth/login")
        );
    }

    @Test
    void shouldRejectInvalidLoginBody()
        throws Exception {

        mockMvc.perform(
            post(BASE_URL + "/login")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content(
                    """
                    {
                      "email": "invalid-email",
                      "password": ""
                    }
                    """
                )
        )
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Validation failed")
        )
        .andExpect(
            jsonPath("$.validationErrors")
                .isArray()
        );

        verify(
            authenticationService,
            never()
        )
        .login(any());
    }

    @Test
    void shouldRefreshTokens()
        throws Exception {

        when(
            authenticationService.refresh(
                any(RefreshTokenRequest.class)
            )
        )
        .thenReturn(
            new AuthResponse(
                "new-access-token",
                "new-refresh-token",
                "Bearer"
            )
        );

        mockMvc.perform(
            post(BASE_URL + "/refresh")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content(
                    """
                    {
                      "refreshToken":
                        "old-refresh-token"
                    }
                    """
                )
        )
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.accessToken")
                .value("new-access-token")
        )
        .andExpect(
            jsonPath("$.refreshToken")
                .value("new-refresh-token")
        )
        .andExpect(
            jsonPath("$.tokenType")
                .value("Bearer")
        );

        verify(authenticationService)
            .refresh(
                any(RefreshTokenRequest.class)
            );
    }

    @Test
    void shouldReturn401ForInvalidRefreshToken()
        throws Exception {

        when(
            authenticationService.refresh(
                any(RefreshTokenRequest.class)
            )
        )
        .thenThrow(
            new InvalidRefreshTokenException()
        );

        mockMvc.perform(
            post(BASE_URL + "/refresh")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content(
                    """
                    {
                      "refreshToken":
                        "invalid-refresh-token"
                    }
                    """
                )
        )
        .andExpect(status().isUnauthorized())
        .andExpect(
            jsonPath("$.status")
                .value(401)
        )
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Invalid or expired refresh token"
                )
        )
        .andExpect(
            jsonPath("$.path")
                .value("/api/v1/auth/refresh")
        );
    }

    @Test
    void shouldRejectBlankRefreshToken()
        throws Exception {

        mockMvc.perform(
            post(BASE_URL + "/refresh")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content(
                    """
                    {
                      "refreshToken": ""
                    }
                    """
                )
        )
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Validation failed")
        )
        .andExpect(
            jsonPath("$.validationErrors")
                .isArray()
        );

        verify(
            authenticationService,
            never()
        )
        .refresh(any());
    }

    @Test
    void shouldReturn500ForUnexpectedFailure()
        throws Exception {

        when(
            authenticationService.login(
                any(LoginRequest.class)
            )
        )
        .thenThrow(
            new IllegalStateException(
                "Unexpected database error"
            )
        );

        mockMvc.perform(
            post(BASE_URL + "/login")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content(
                    """
                    {
                      "email": "user@example.com",
                      "password": "StrongPassword123"
                    }
                    """
                )
        )
        .andExpect(
            status().isInternalServerError()
        )
        .andExpect(
            jsonPath("$.status")
                .value(500)
        )
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Internal server error"
                )
        );
    }
}