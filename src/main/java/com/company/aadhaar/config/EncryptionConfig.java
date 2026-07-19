package com.company.aadhaar.config;



import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;


@Configuration
public class EncryptionConfig {

    @Value("${encryption.key:}")
    private String encryptionKey;

    /**
     * Returns an AES key for AES-GCM.
     *
     * Expected formats:
     * 1) Base64 encoded 16/24/32 bytes
     * 2) Raw string (will be UTF-8 bytes; must be 16/24/32 bytes after encoding)
     */
    public SecretKey getAesKeyOrThrow() {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            throw new IllegalStateException("Missing encryption.key (base64 of 16/24/32 bytes)");
        }

        byte[] keyBytes;
        try {
            // Try base64 first
            keyBytes = java.util.Base64.getDecoder().decode(encryptionKey);
        } catch (IllegalArgumentException ignore) {
            // Fallback: treat as raw UTF-8 string
            keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        }

        int len = keyBytes.length;
        if (len != 16 && len != 24 && len != 32) {
            throw new IllegalStateException(
                    "Invalid AES key length: " + len + " bytes. Must be 16/24/32 bytes for AES-128/192/256-GCM"
            );
        }

        return new SecretKeySpec(keyBytes, "AES");
    }
}

