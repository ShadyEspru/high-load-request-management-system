package com.hlrms.authservice.controller;

import com.hlrms.authservice.dto.request.LoginRequest;
import com.hlrms.authservice.dto.request.RefreshTokenRequest;
import com.hlrms.authservice.dto.request.RegisterRequest;
import com.hlrms.authservice.dto.response.AuthResponse;
import com.hlrms.authservice.dto.response.RegisterResponse;
import com.hlrms.authservice.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(
            AuthenticationService authenticationService
    ) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return authenticationService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authenticationService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return authenticationService.refresh(request);
    }
}