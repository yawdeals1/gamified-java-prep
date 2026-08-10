package com.gamifiedjava.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class ApiKeyCipher {
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    public ApiKeyCipher(@Value("${app.encryption-key:}") String secret) {
        if (secret == null || secret.length() < 32) {
            this.key = null;
        } else {
            try {
                this.key = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to initialize API key encryption", e);
            }
        }
    }

    public boolean isConfigured() { return key != null; }

    public String encrypt(String value) {
        requireConfigured();
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return "v1." + Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encrypt API key", e);
        }
    }

    public String decrypt(String stored) {
        requireConfigured();
        try {
            if (stored == null || !stored.startsWith("v1.")) throw new IllegalArgumentException("Unknown key format");
            byte[] combined = Base64.getUrlDecoder().decode(stored.substring(3));
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_BYTES);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_BYTES, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to decrypt API key", e);
        }
    }

    private void requireConfigured() {
        if (!isConfigured()) throw new IllegalStateException("AI key encryption is not configured");
    }
}
