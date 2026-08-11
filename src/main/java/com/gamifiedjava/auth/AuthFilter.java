package com.gamifiedjava.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@Order(10)
public class AuthFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login", "/auth/invite", "/auth/confirm",
            "/auth/logout", "/auth/status", "/error", "/favicon.ico"
    );

    private final AuthService authService;
    private final MembershipService membershipService;
    private final CurrentUserContext currentUserContext;

    public AuthFilter(AuthService authService, MembershipService membershipService,
                      CurrentUserContext currentUserContext) {
        this.authService = authService;
        this.membershipService = membershipService;
        this.currentUserContext = currentUserContext;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/fonts/")
                || path.startsWith("/images/") || path.startsWith("/img/")
                || path.startsWith("/webjars/")) return true;
        return PUBLIC_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!authService.isConfigured()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType(request.getRequestURI().startsWith("/api/")
                    ? "application/json" : "text/plain;charset=UTF-8");
            response.getWriter().write(request.getRequestURI().startsWith("/api/")
                    ? "{\"error\":\"Authentication service unavailable\"}"
                    : "Authentication service unavailable.");
            return;
        }
        String token = readCookie(request, authService.cookieName());
        if (token != null) {
            AuthUser user = authService.validate(token);
            if (user != null) {
                var member = membershipService.resolve(user);
                if (member.isPresent()) {
                    request.setAttribute("authUser", user);
                    request.setAttribute("appUser", member.get());
                    request.setAttribute("isAdmin", member.get().getRole() == com.gamifiedjava.model.AppUser.Role.ADMIN);
                    currentUserContext.set(user.id());
                    try {
                        filterChain.doFilter(request, response);
                    } finally {
                        currentUserContext.clear();
                    }
                    return;
                }
            }
        }

        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }
        response.sendRedirect("/auth/login");
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
