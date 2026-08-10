package com.gamifiedjava.model;

import java.time.LocalDateTime;

public class UserAiSettings {
    private Integer id;
    private String authUserId;
    private String encryptedApiKey;
    private String keyLastFour;
    private LocalDateTime updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getAuthUserId() { return authUserId; }
    public void setAuthUserId(String authUserId) { this.authUserId = authUserId; }
    public String getEncryptedApiKey() { return encryptedApiKey; }
    public void setEncryptedApiKey(String encryptedApiKey) { this.encryptedApiKey = encryptedApiKey; }
    public String getKeyLastFour() { return keyLastFour; }
    public void setKeyLastFour(String keyLastFour) { this.keyLastFour = keyLastFour; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
