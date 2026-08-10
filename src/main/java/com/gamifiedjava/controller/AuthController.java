package com.gamifiedjava.controller;

import com.gamifiedjava.auth.AuthService;
import com.gamifiedjava.auth.AuthUser;
import com.gamifiedjava.auth.MembershipService;
import com.gamifiedjava.service.InvitationService;
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

@Controller
public class AuthController {

    private final AuthService authService;
    private final MembershipService membershipService;
    private final InvitationService invitationService;

    public AuthController(AuthService authService, MembershipService membershipService,
                          InvitationService invitationService) {
        this.authService = authService;
        this.membershipService = membershipService;
        this.invitationService = invitationService;
    }

    @GetMapping("/auth/login")
    public String loginPage(HttpServletRequest request) {
        if (request.getAttribute("authUser") != null) return "redirect:/";
        return "auth/login";
    }

    @GetMapping("/auth/confirm")
    public String confirmPage() {
        return "auth/confirm";
    }

    @GetMapping("/auth/invite")
    public String invitePage(@RequestParam String token, Model model) {
        var invitation = invitationService.valid(token);
        if (invitation.isEmpty()) {
            model.addAttribute("invalidInvite", true);
            return "auth/invite";
        }
        model.addAttribute("email", invitation.get().getEmail());
        model.addAttribute("token", token);
        return "auth/invite";
    }

    @PostMapping("/auth/invite")
    public String acceptInvite(@RequestParam String token,
                               @RequestParam String password,
                               @RequestParam String password2,
                               @RequestParam String name,
                               Model model) {
        var invitation = invitationService.valid(token);
        if (invitation.isEmpty()) {
            model.addAttribute("invalidInvite", true);
            return "auth/invite";
        }
        model.addAttribute("email", invitation.get().getEmail());
        model.addAttribute("token", token);
        if (name == null || name.trim().length() < 2) {
            model.addAttribute("error", "Please enter your name.");
            return "auth/invite";
        }
        if (password == null || password.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters.");
            return "auth/invite";
        }
        if (!password.equals(password2)) {
            model.addAttribute("error", "Passwords do not match.");
            return "auth/invite";
        }
        if (!authService.signup(invitation.get().getEmail(), password, name.trim())) {
            model.addAttribute("error", "Account creation is temporarily unavailable. Please try again.");
            return "auth/invite";
        }
        invitationService.accept(token, name.trim());
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
        if (membershipService.resolve(result.user()).isEmpty()) {
            model.addAttribute("error", "This account has not been invited or its access was removed.");
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
