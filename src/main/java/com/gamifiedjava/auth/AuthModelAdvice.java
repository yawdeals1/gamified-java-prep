package com.gamifiedjava.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AuthModelAdvice {

    @ModelAttribute("currentUser")
    public AuthUser currentUser(HttpServletRequest request) {
        return (AuthUser) request.getAttribute("authUser");
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute("isAdmin"));
    }
}
