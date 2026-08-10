package com.gamifiedjava.auth;

import com.gamifiedjava.model.AppUser;
import com.gamifiedjava.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MembershipServiceTest {
    private static final String OWNER_ID = "owner-auth-id";
    private static final String OWNER_EMAIL = "owner@example.com";

    private AppUserRepository repository;
    private MembershipService service;

    @BeforeEach
    void setUp() {
        repository = mock(AppUserRepository.class);
        service = new MembershipService(repository, OWNER_ID, OWNER_EMAIL);
    }

    @Test
    void invitedAccountsAreAlwaysSavedAsMembers() {
        AppUser staleAdmin = user("member-auth-id", "member@example.com", AppUser.Role.ADMIN);
        when(repository.findByEmail("member@example.com")).thenReturn(Optional.of(staleAdmin));

        AppUser saved = service.addMember("MEMBER@example.com", "Member");

        assertThat(saved.getRole()).isEqualTo(AppUser.Role.MEMBER);
        verify(repository).save(staleAdmin);
    }

    @Test
    void nonOwnerAdminRoleIsDemotedDuringAuthentication() {
        AppUser staleAdmin = user("member-auth-id", "member@example.com", AppUser.Role.ADMIN);
        when(repository.findByAuthUserId("member-auth-id")).thenReturn(Optional.of(staleAdmin));

        AppUser resolved = service.resolve(new AuthUser(
                "member-auth-id", "member@example.com", "Member", "email_password")).orElseThrow();

        assertThat(resolved.getRole()).isEqualTo(AppUser.Role.MEMBER);
        assertThat(service.isAdmin(new AuthUser(
                "member-auth-id", "member@example.com", "Member", "email_password"))).isFalse();
        verify(repository).save(staleAdmin);
    }

    @Test
    void ownerAuthIdRetainsAdminRole() {
        AppUser owner = user(OWNER_ID, OWNER_EMAIL, AppUser.Role.ADMIN);
        when(repository.findByAuthUserId(OWNER_ID)).thenReturn(Optional.of(owner));

        AppUser resolved = service.resolve(new AuthUser(
                OWNER_ID, OWNER_EMAIL, "Owner", "email_password")).orElseThrow();

        assertThat(resolved.getRole()).isEqualTo(AppUser.Role.ADMIN);
        assertThat(service.isAdmin(new AuthUser(
                OWNER_ID, OWNER_EMAIL, "Owner", "email_password"))).isTrue();
    }

    @Test
    void activeUserListingNormalizesEveryNonOwnerToMember() {
        AppUser owner = user(OWNER_ID, OWNER_EMAIL, AppUser.Role.ADMIN);
        AppUser member = user("other-id", "other@example.com", AppUser.Role.ADMIN);
        when(repository.findActive()).thenReturn(List.of(owner, member));

        List<AppUser> users = service.activeUsers();

        assertThat(users).extracting(AppUser::getRole)
                .containsExactly(AppUser.Role.ADMIN, AppUser.Role.MEMBER);
        verify(repository).save(member);
    }

    private static AppUser user(String authId, String email, AppUser.Role role) {
        AppUser user = new AppUser();
        user.setAuthUserId(authId);
        user.setEmail(email);
        user.setRole(role);
        user.setActive(true);
        return user;
    }
}
