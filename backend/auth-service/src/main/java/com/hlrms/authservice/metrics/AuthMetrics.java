package com.hlrms.authservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AuthMetrics {

    private final Counter registerSuccess;
    private final Counter loginSuccess;
    private final Counter loginFailed;
    private final Counter refreshSuccess;

    public AuthMetrics(MeterRegistry registry) {

        this.registerSuccess = Counter.builder("hlrms_auth_register_success")
                .description("Successful user registrations")
                .register(registry);

        this.loginSuccess = Counter.builder("hlrms_auth_login_success")
                .description("Successful user logins")
                .register(registry);

        this.loginFailed = Counter.builder("hlrms_auth_login_failed")
                .description("Failed user logins")
                .register(registry);

        this.refreshSuccess = Counter.builder("hlrms_auth_refresh_success")
                .description("Successful refresh token operations")
                .register(registry);
    }

    public void registerSuccess() {
        registerSuccess.increment();
    }

    public void loginSuccess() {
        loginSuccess.increment();
    }

    public void loginFailed() {
        loginFailed.increment();
    }

    public void refreshSuccess() {
        refreshSuccess.increment();
    }
}