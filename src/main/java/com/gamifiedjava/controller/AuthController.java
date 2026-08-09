package com.gamifiedjava.controller;

import com.gamifiedjava.auth.AuthService;
import com.gamifiedjava.auth.AuthUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Controller
public class AuthController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/auth/login")
    public String loginPage(HttpServletRequest request) {
        if (request.getAttribute("authUser") != null) return "redirect:/";
        return "auth/login";
    }

    @GetMapping("/auth/signup")
    public String signupPage(HttpServletRequest request) {
        if (request.getAttribute("authUser") != null) return "redirect:/";
        return "auth/signup";
    }

    @GetMapping("/auth/confirm")
    public String confirmPage() {
        return "auth/confirm";
    }

    @PostMapping("/auth/signup")
    public String signup(@RequestParam String email,
                         @RequestParam String password,
                         @RequestParam(required = false) String name,
                         Model model) {
        String cleaned = email == null ? "" : email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(cleaned).matches()) {
            model.addAttribute("error", "Please enter a valid email address.");
            return "auth/signup";
        }
        if (password == null || password.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters.");
            return "auth/signup";
        }
        authService.signup(cleaned, password, name);
        return "redirect:/auth/confirm";
    }

    @PostMapping("/auth/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Model model) {
        if (!authService.isConfigured()) {
            model.addAttribute("error", "Authentication is not configured on this server yet.");
            return "auth/login";
        }
        AuthService.LoginResult result = authService.login(
                email == null ? "" : email.trim().toLowerCase(),
                password == null ? "" : password);
        if (result.user() == null) {
            model.addAttribute("error", result.error());
            return "auth/login";
        }

        Cookie cookie = new Cookie(authService.cookieName(), result.sessionToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);
        return "redirect:/";
    }

    @PostMapping("/auth/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        String token = readCookie(request, authService.cookieName());
        if (token != null) authService.logout(token);
        Cookie cookie = new Cookie(authService.cookieName(), "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/auth/login";
    }

    @GetMapping("/auth/status")
    @ResponseBody
    public Map<String, Object> status(HttpServletRequest request) {
        AuthUser user = (AuthUser) request.getAttribute("authUser");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("authenticated", user != null);
        out.put("configured", authService.isConfigured());
        if (user != null) {
            out.put("email", user.email());
            out.put("name", user.name());
        }
        return out;
    }

    private String readCookie(HttpServletRequest request, String name) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (jakarta.servlet.http.Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}