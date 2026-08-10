package com.gamifiedjava.model;

import java.time.LocalDateTime;

public class MemberInvitation {

    public enum Status { PENDING, ACCEPTED, REVOKED, DELIVERY_FAILED }

    private Integer id;
    private String email;
    private String tokenHash;
    private Status status = Status.PENDING;
    private String invitedBy;
    private LocalDateTime expiresAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getInvitedBy() { return invitedBy; }
    public void setInvitedBy(String invitedBy) { this.invitedBy = invitedBy; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isUsable() {
        return status == Status.PENDING && expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
    }
}
