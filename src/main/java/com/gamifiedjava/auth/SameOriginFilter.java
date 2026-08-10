package com.gamifiedjava.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

/** Rejects cross-origin state-changing requests before authentication is evaluated. */
@Component
@Order(5)
public class SameOriginFilter extends OncePerRequestFilter {
    private static final Set<String> SAFE = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private final String publicOrigin;

    public SameOriginFilter(@Value("${app.public-base-url:}") String publicBaseUrl) {
        this.publicOrigin = originOf(publicBaseUrl);
    }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return SAFE.contains(request.getMethod().toUpperCase());
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                              FilterChain chain) throws ServletException, IOException {
        String fetchSite = request.getHeader("Sec-Fetch-Site");
        if ("cross-site".equalsIgnoreCase(fetchSite)) {
            reject(response);
            return;
        }
        String supplied = originOf(request.getHeader("Origin"));
        if (supplied.isBlank()) supplied = originOf(request.getHeader("Referer"));
        String requestOrigin = request.getScheme() + "://" + request.getServerName()
                + portSuffix(request.getScheme(), request.getServerPort());
        if (supplied.isBlank() || (!supplied.equalsIgnoreCase(requestOrigin)
                && (publicOrigin.isBlank() || !supplied.equalsIgnoreCase(publicOrigin)))) {
            reject(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Cross-origin request rejected\"}");
    }

    private static String originOf(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() == null || uri.getHost() == null) return "";
            return uri.getScheme().toLowerCase() + "://" + uri.getHost().toLowerCase()
                    + portSuffix(uri.getScheme(), uri.getPort());
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private static String portSuffix(String scheme, int port) {
        if (port < 0 || ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443)) return "";
        return ":" + port;
    }
}
