package com.gamifiedjava.auth;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SameOriginFilterTest {
    private final SameOriginFilter filter = new SameOriginFilter("https://java.example");

    @Test
    void acceptsMatchingOrigin() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/run");
        request.setScheme("https");
        request.setServerName("java.example");
        request.setServerPort(443);
        request.addHeader("Origin", "https://java.example");
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsMissingOrigin() throws Exception {
        var request = new MockHttpServletRequest("POST", "/reset/progress");
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verifyNoInteractions(chain);
    }
}
