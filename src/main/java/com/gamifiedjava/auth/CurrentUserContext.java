package com.gamifiedjava.auth;

import org.springframework.stereotype.Component;

import java.util.Optional;

/** Carries the authenticated user id through request-owned virtual threads. */
@Component
public class CurrentUserContext {
    private final InheritableThreadLocal<String> authUserId = new InheritableThreadLocal<>();

    public void set(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Authenticated user id is required.");
        }
        authUserId.set(userId);
    }

    public void clear() { authUserId.remove(); }

    public Optional<String> currentUserId() {
        return Optional.ofNullable(authUserId.get()).filter(id -> !id.isBlank());
    }

    public String requireUserId() {
        return currentUserId().orElseThrow(
                () -> new IllegalStateException("No authenticated user is bound to this request."));
    }
}
