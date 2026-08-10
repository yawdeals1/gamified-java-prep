package com.gamifiedjava.auth;

import com.gamifiedjava.model.AppUser;
import com.gamifiedjava.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MembershipService {
    private static final long CACHE_MILLIS = 30_000;
    private record Cached(Optional<AppUser> user, long expiresAt) {}

    private final AppUserRepository repository;
    private final String adminAuthUserId;
    private final String adminEmail;
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    public MembershipService(
            AppUserRepository repository,
            @Value("${app.admin-auth-user-id}") String adminAuthUserId,
            @Value("${app.admin-email}") String adminEmail) {
        this.repository = repository;
        this.adminAuthUserId = clean(adminAuthUserId);
        this.adminEmail = normalize(adminEmail);
    }

    public Optional<AppUser> resolve(AuthUser authUser) {
        if (authUser == null) return Optional.empty();
        String key = authUser.id() == null || authUser.id().isBlank()
                ? normalize(authUser.email()) : authUser.id();
        Cached hit = cache.get(key);
        long now = System.currentTimeMillis();
        if (hit != null && hit.expiresAt() > now) return hit.user();

        Optional<AppUser> found = Optional.empty();
        if (authUser.id() != null && !authUser.id().isBlank()) {
            found = repository.findByAuthUserId(authUser.id());
        }
        if (found.isEmpty() && authUser.email() != null) {
            found = repository.findByEmail(normalize(authUser.email()));
        }
        found.ifPresent(member -> bindIdentityAndEnforceRole(member, authUser));
        Optional<AppUser> active = found.filter(AppUser::isActive);
        cache.put(key, new Cached(active, now + CACHE_MILLIS));
        return active;
    }

    public boolean isAdmin(AuthUser user) {
        return resolve(user).map(AppUser::getRole).orElse(null) == AppUser.Role.ADMIN;
    }

    public AppUser addMember(String email, String name) {
        String clean = normalize(email);
        AppUser member = repository.findByEmail(clean).orElseGet(AppUser::new);
        member.setEmail(clean);
        member.setDisplayName(name);
        member.setRole(AppUser.Role.MEMBER);
        member.setActive(true);
        member.setUpdatedAt(LocalDateTime.now());
        repository.save(member);
        cache.clear();
        return member;
    }

    public List<AppUser> activeUsers() {
        List<AppUser> users = repository.findActive();
        users.forEach(this::enforceStoredRole);
        return users;
    }

    public Optional<AppUser> findById(Integer id) { return repository.findById(id); }

    public void deactivate(AppUser member) {
        member.setActive(false);
        member.setUpdatedAt(LocalDateTime.now());
        repository.save(member);
        cache.clear();
    }

    private void bindIdentityAndEnforceRole(AppUser member, AuthUser authUser) {
        boolean changed = false;
        if (member.getAuthUserId() == null || member.getAuthUserId().isBlank()) {
            member.setAuthUserId(authUser.id());
            changed = true;
            if (member.getDisplayName() == null || member.getDisplayName().isBlank()) {
                member.setDisplayName(authUser.name());
            }
        }
        AppUser.Role allowedRole = isOwner(member.getAuthUserId(), member.getEmail())
                ? AppUser.Role.ADMIN : AppUser.Role.MEMBER;
        if (member.getRole() != allowedRole) {
            member.setRole(allowedRole);
            changed = true;
        }
        if (changed) {
            member.setUpdatedAt(LocalDateTime.now());
            repository.save(member);
        }
    }

    private void enforceStoredRole(AppUser member) {
        AppUser.Role allowedRole = isOwner(member.getAuthUserId(), member.getEmail())
                ? AppUser.Role.ADMIN : AppUser.Role.MEMBER;
        if (member.getRole() != allowedRole) {
            member.setRole(allowedRole);
            member.setUpdatedAt(LocalDateTime.now());
            repository.save(member);
            cache.clear();
        }
    }

    private boolean isOwner(String authUserId, String email) {
        String cleanAuthUserId = clean(authUserId);
        if (!cleanAuthUserId.isBlank()) {
            return !adminAuthUserId.isBlank() && adminAuthUserId.equals(cleanAuthUserId);
        }
        return !adminEmail.isBlank() && adminEmail.equals(normalize(email));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
