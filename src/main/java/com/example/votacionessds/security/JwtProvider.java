package com.example.votacionessds.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    public static final String SESSION_CLAIM = "sid";

    @Value("${security.jwt.secret-key}")
    private String jwtSecret;

    @Value("${security.jwt.expiration}")
    private long jwtExpiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** Claims ya validados de un access token: username + sessionId (si presente). */
    public record TokenClaims(String username, UUID sessionId) {}

    public String generateAccessToken(UserDetails userDetails, UUID sessionId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpiration);
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(exp)
                .claim(SESSION_CLAIM, sessionId.toString())
                // Algoritmo explícito: nunca dejarlo inferido del tamaño de la clave.
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parsea y valida el token UNA sola vez (firma + expiración). Reemplaza a
     * validateToken()/getUsernameFromToken()/getSessionIdFromToken() por separado,
     * que triplicaban la verificación criptográfica del mismo token por request.
     */
    public Optional<TokenClaims> parseValidClaims(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String username = claims.getSubject();
            Object sidClaim = claims.get(SESSION_CLAIM);
            UUID sessionId = sidClaim != null ? UUID.fromString(sidClaim.toString()) : null;
            return Optional.of(new TokenClaims(username, sessionId));
        } catch (JwtException | IllegalArgumentException ex) {
            // No exponer el token ni la causa detallada al cliente; sí dejar rastro para auditoría.
            log.debug("Token JWT rechazado: {}", ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
