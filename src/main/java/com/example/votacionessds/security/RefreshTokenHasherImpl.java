package com.example.votacionessds.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RefreshTokenHasherImpl implements RefreshTokenHasher {

    @Override
    public String generateRawToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Hash determinístico (mismo input -> mismo output): necesario para poder
     * buscar el token por igualdad exacta en BD (WHERE token_hash = ?).
     * NUNCA loguear ni el rawToken ni el hash resultante — ver JwtAuthenticationFilter
     * y RefreshTokenServiceImpl para el manejo correcto de logs de seguridad.
     */
    @Override
    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
