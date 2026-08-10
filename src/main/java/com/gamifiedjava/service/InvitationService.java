package com.gamifiedjava.service;

import com.gamifiedjava.auth.MembershipService;
import com.gamifiedjava.model.MemberInvitation;
import com.gamifiedjava.repository.MemberInvitationRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class InvitationService {
    public record Created(MemberInvitation invitation, String token) {}

    private final MemberInvitationRepository repository;
    private final CloudflareInviteEmailService emailService;
    private final MembershipService membershipService;
    private final SecureRandom random = new SecureRandom();

    public InvitationService(MemberInvitationRepository repository,
                             CloudflareInviteEmailService emailService,
                             MembershipService membershipService) {
        this.repository = repository;
        this.emailService = emailService;
        this.membershipService = membershipService;
    }

    public Created invite(String email, String invitedBy, String baseUrl) {
        String clean = MembershipService.normalize(email);
        if (membershipService.activeUsers().stream().anyMatch(u -> clean.equals(u.getEmail()))) {
            throw new IllegalArgumentException("That email already belongs to an active user.");
        }
        for (MemberInvitation pending : repository.findPending()) {
            if (clean.equals(pending.getEmail())) {
                pending.setStatus(MemberInvitation.Status.REVOKED);
                repository.save(pending);
            }
        }

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        MemberInvitation invitation = new MemberInvitation();
        invitation.setEmail(clean);
        invitation.setTokenHash(hash(token));
        invitation.setInvitedBy(invitedBy);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(48));
        repository.save(invitation);

        try {
            emailService.sendInvite(clean, stripSlash(baseUrl) + "/auth/invite?token=" + token);
        } catch (RuntimeException e) {
            invitation.setStatus(MemberInvitation.Status.DELIVERY_FAILED);
            repository.save(invitation);
            throw e;
        }
        return new Created(invitation, token);
    }

    public Optional<MemberInvitation> valid(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return repository.findByTokenHash(hash(token)).filter(MemberInvitation::isUsable);
    }

    public MemberInvitation accept(String token, String name) {
        MemberInvitation invitation = valid(token)
                .orElseThrow(() -> new IllegalArgumentException("This invitation is invalid or has expired."));
        invitation.setStatus(MemberInvitation.Status.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        repository.save(invitation);
        membershipService.addMember(invitation.getEmail(), name);
        return invitation;
    }

    public List<MemberInvitation> pending() { return repository.findPending(); }
    public boolean emailReady() { return emailService.isConfigured(); }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to secure invitation token", e);
        }
    }
    private static String stripSlash(String url) {
        String clean = url == null ? "" : url.trim();
        while (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
        return clean;
    }
}
