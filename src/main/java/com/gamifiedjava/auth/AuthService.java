package com.gamifiedjava.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final long SESSION_CACHE_MILLIS = 30_000;

    private record CachedSession(AuthUser user, long expiresAt) {}

    public record LoginResult(AuthUser user, String sessionToken, String error) {
        static LoginResult success(String token, AuthUser user) {
            return new LoginResult(user, token, null);
        }

        static LoginResult failure(String error) {
            return new LoginResult(null, null, error != null ? error : "Invalid email or password.");
        }
    }

    private final RestClient restClient = RestClient.builder().build();
    private final String baseUrl;
    private final String slug;
    private final Map<String, CachedSession> sessionCache = new ConcurrentHashMap<>();

    public AuthService(@Value("${deploro.auth.base-url:}") String baseUrl,
                       @Value("${deploro.auth.slug:}") String slug) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        this.baseUrl = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        this.slug = slug == null ? "" : slug.trim();
    }

    public boolean isConfigured() {
        return !baseUrl.isBlank() && !slug.isBlank();
    }

    public String cookieName() {
        return "gallium_project_session_" + slug;
    }

    private URI authUri(String path) {
        return URI.create(baseUrl + "/auth/" + slug + path);
    }

    public boolean signup(String email, String password, String name) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        if (name != null && !name.isBlank()) body.put("name", name);
        try {
            restClient.post().uri(authUri("/email-password/signup")).body(body).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientResponseException ignored) {
            return false;
        }
    }

    public LoginResult login(String email, String password) {
        Map<String, Object> body = Map.of("email", email, "password", password);
        try {
            ResponseEntity<Map> resp = restClient.post()
                    .uri(authUri("/email-password/login"))
                    .body(body)
                    .retrieve()
                    .toEntity(Map.class);
            Map json = resp.getBody();
            if (json != null && json.get("user") instanceof Map<?, ?> userJson) {
                String token = extractSessionToken(resp.getHeaders().get(HttpHeaders.SET_COOKIE));
                if (token == null) return LoginResult.failure("Sign-in succeeded but no session cookie was returned.");
                return LoginResult.success(token, toUser((Map<String, Object>) userJson));
            }
            Object error = json != null ? json.get("error") : null;
            return LoginResult.failure(error != null ? error.toString() : null);
        } catch (HttpClientErrorException e) {
            Object error = readError(e);
            if (error == null && e.getStatusCode().value() == 429) {
                return LoginResult.failure("Too many attempts. Try again later.");
            }
            return LoginResult.failure(error != null ? error.toString() : "Invalid email or password.");
        } catch (RestClientResponseException e) {
            return LoginResult.failure("Sign-in is temporarily unavailable. Please try again.");
        }
    }

    private Object readError(HttpClientErrorException e) {
        try {
            Map body = e.getResponseBodyAs(Map.class);
            return body != null ? body.get("error") : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public AuthUser validate(String token) {
        CachedSession cached = sessionCache.get(token);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiresAt() > now) return cached.user();
        if (cached != null) sessionCache.remove(token, cached);

        try {
            Map json = restClient.get()
                    .uri(authUri("/session"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(Map.class);
            if (json == null || !(json.get("user") instanceof Map<?, ?> userJson)) return null;
            AuthUser user = toUser((Map<String, Object>) userJson);
            sessionCache.put(token, new CachedSession(user, now + SESSION_CACHE_MILLIS));
            return user;
        } catch (HttpClientErrorException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public void logout(String token) {
        sessionCache.remove(token);
        try {
            restClient.post()
                    .uri(authUri("/logout"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ignored) {
            // best-effort; the local cookie is cleared regardless
        }
    }

    private String extractSessionToken(List<String> setCookies) {
        if (setCookies == null) return null;
        String prefix = cookieName() + "=";
        for (String cookie : setCookies) {
            if (cookie != null && cookie.startsWith(prefix)) {
                String value = cookie.substring(prefix.length());
                int semi = value.indexOf(';');
                return semi >= 0 ? value.substring(0, semi) : value;
            }
        }
        return null;
    }

    private AuthUser toUser(Map<String, Object> json) {
        return new AuthUser(
                str(json.get("id")),
                str(json.get("email")),
                str(json.get("name")),
                str(json.get("provider"))
        );
    }

    private String str(Object o) {
        return o != null ? o.toString() : null;
    }
}
