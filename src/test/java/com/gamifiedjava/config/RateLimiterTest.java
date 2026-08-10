package com.gamifiedjava.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {
    @Test
    void enforcesAuthenticationAttemptLimit() {
        RateLimiter limiter = new RateLimiter();
        for (int i = 0; i < 10; i++) {
            RateLimiter.Lease lease = limiter.tryAcquire("auth", "192.0.2.1");
            assertThat(lease).isNotNull();
            lease.close();
        }
        assertThat(limiter.tryAcquire("auth", "192.0.2.1")).isNull();
    }
}
