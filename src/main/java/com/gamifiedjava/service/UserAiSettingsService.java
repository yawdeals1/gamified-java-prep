package com.gamifiedjava.service;

import com.gamifiedjava.model.UserAiSettings;
import com.gamifiedjava.repository.UserAiSettingsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserAiSettingsService {
    private final UserAiSettingsRepository repository;
    private final ApiKeyCipher cipher;

    public UserAiSettingsService(UserAiSettingsRepository repository, ApiKeyCipher cipher) {
        this.repository = repository;
        this.cipher = cipher;
    }

    public Optional<UserAiSettings> settings(String authUserId) {
        if (authUserId == null || authUserId.isBlank()) return Optional.empty();
        return repository.findByAuthUserId(authUserId);
    }

    public Optional<String> apiKey(String authUserId) {
        return settings(authUserId).map(UserAiSettings::getEncryptedApiKey).map(cipher::decrypt);
    }

    public UserAiSettings save(String authUserId, String apiKey) {
        if (!cipher.isConfigured()) throw new IllegalStateException("Server-side encryption is not configured.");
        String clean = apiKey == null ? "" : apiKey.trim();
        if (clean.length() < 12 || clean.length() > 512) throw new IllegalArgumentException("Enter a valid Ollama API key.");
        UserAiSettings settings = repository.findByAuthUserId(authUserId).orElseGet(UserAiSettings::new);
        settings.setAuthUserId(authUserId);
        settings.setEncryptedApiKey(cipher.encrypt(clean));
        settings.setKeyLastFour(clean.substring(clean.length() - 4));
        settings.setUpdatedAt(LocalDateTime.now());
        return repository.save(settings);
    }

    public void remove(String authUserId) {
        repository.findByAuthUserId(authUserId).ifPresent(s -> repository.deleteAll(java.util.List.of(s)));
    }

    public boolean encryptionReady() { return cipher.isConfigured(); }
}
