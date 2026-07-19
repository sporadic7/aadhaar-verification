package com.company.aadhaar.util;

import com.company.aadhaar.config.EncryptionConfig;
import com.company.aadhaar.exception.EncryptionException;



import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM authenticated encryption.
 * <p>
 * Encoding format returned by encrypt(): base64(iv) + ":" + base64(ciphertext_with_tag)
 */
public final class EncryptionUtil {


    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12; // NIST recommended for GCM
    private static final int TAG_LENGTH_BITS = 128;

    private final EncryptionConfig encryptionConfig;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionUtil(EncryptionConfig encryptionConfig) {
        this.encryptionConfig = encryptionConfig;
    }

    public String encrypt(String data) {
        if (data == null) return null;
        try {
            SecretKey key = encryptionConfig.getAesKeyOrThrow();

            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            byte[] plaintext = data.getBytes(StandardCharsets.UTF_8);
            byte[] cipherBytes = cipher.doFinal(plaintext);

            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cipherBytes);
        } catch (IllegalStateException e) {
            throw new EncryptionException("Encryption key misconfigured: " + e.getMessage(), e);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Encryption failed", e);
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null) return null;
        try {
            SecretKey key = encryptionConfig.getAesKeyOrThrow();

            String[] parts = encrypted.split(":", 2);
            if (parts.length != 2) {
                throw new EncryptionException("Invalid encrypted payload format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);

            if (iv.length != IV_LENGTH_BYTES) {
                throw new EncryptionException("Invalid IV length: " + iv.length);
            }

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            byte[] plaintext = cipher.doFinal(cipherBytes);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw new EncryptionException("Encryption key misconfigured: " + e.getMessage(), e);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Decryption failed", e);
        }
    }
}

