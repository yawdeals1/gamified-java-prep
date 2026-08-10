package com.gamifiedjava.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Public deployments fail at startup instead of silently disabling security. */
@Component
public class SecurityConfigurationValidator implements ApplicationRunner {
    private final String bindAddress;
    private final String authBaseUrl;
    private final String authSlug;
    private final String encryptionKey;

    public SecurityConfigurationValidator(
            @Value("${server.address:127.0.0.1}") String bindAddress,
            @Value("${deploro.auth.base-url:}") String authBaseUrl,
            @Value("${deploro.auth.slug:}") String authSlug,
            @Value("${app.encryption-key:}") String encryptionKey) {
        this.bindAddress = bindAddress;
        this.authBaseUrl = authBaseUrl;
        this.authSlug = authSlug;
        this.encryptionKey = encryptionKey;
    }

    @Override public void run(ApplicationArguments args) {
        if (isLoopback(bindAddress)) return;
        if (blank(authBaseUrl) || blank(authSlug)) {
            throw new IllegalStateException("Public deployment requires Deploro authentication configuration.");
        }
        if (blank(encryptionKey)) {
            throw new IllegalStateException("Public deployment requires APP_ENCRYPTION_KEY.");
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private boolean isLoopback(String value) {
        if (value == null) return false;
        return java.util.Set.of("127.0.0.1", "localhost", "::1", "0:0:0:0:0:0:0:1")
                .contains(value.strip().toLowerCase());
    }
}
