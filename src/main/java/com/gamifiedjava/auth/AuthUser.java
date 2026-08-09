package com.gamifiedjava.auth;

public record AuthUser(String id, String email, String name, String provider) {

    public String displayName() {
        return (name == null || name.isBlank()) ? email : name;
    }
}