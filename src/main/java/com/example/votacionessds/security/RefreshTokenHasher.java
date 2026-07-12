package com.example.votacionessds.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public final class RefreshTokenHasher {

    private RefreshTokenHasher() {
    }

    public static String generateRawToken() {
        System.out.println("RefreshTokenHasher: generateRawToken");
        return UUID.randomUUID().toString();
    }

    public static String hash(String rawToken) {
        System.out.println("RefreshTokenHasher: hash");
        System.out.println("rawToken: " + rawToken);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
