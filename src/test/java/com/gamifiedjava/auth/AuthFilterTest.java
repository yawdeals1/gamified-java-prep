package com.gamifiedjava.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthFilterTest {

    private AuthFilter filter;

    @BeforeEach
    void setUp() {
        AuthService authService = mock(AuthService.class);
        when(authService.isConfigured()).thenReturn(true);
        filter = new AuthFilter(authService, mock(MembershipService.class));
    }

    @Test
    void fontAssetsArePublic() {
        var request = new MockHttpServletRequest("GET", "/fonts/material-symbols-subset.woff2");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void applicationPagesRemainProtected() {
        var request = new MockHttpServletRequest("GET", "/");

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void invitationAcceptanceIsPublicButOpenSignupIsNot() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/auth/invite"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/auth/signup"))).isFalse();
    }
}
