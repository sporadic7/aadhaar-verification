package com.company.aadhaar.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class CodeChallengeUtil {

    private CodeChallengeUtil() {}

    /**
     * Generates a RFC7636 code_verifier using random bytes and Base64URL (no padding).
     * Length is typically between 43-128 characters.
     */
    public static String generateCodeVerifier() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generates RFC7636 code_challenge for S256.
     */
    public static String generateCodeChallengeS256(String verifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(verifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PKCE S256 code challenge", e);
        }
    }
}

