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

    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(userDetails, null);
    }

    public String generateAccessToken(UserDetails userDetails, UUID sessionId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpiration);
        var builder = Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(exp);
        if (sessionId != null) {
            builder.claim(SESSION_CLAIM, sessionId.toString());
        }
        return builder.signWith(key).compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key).build().parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }

    public Optional<UUID> getSessionIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key).build().parseSignedClaims(token).getPayload();
        Object sid = claims.get(SESSION_CLAIM);
        if (sid == null) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(sid.toString()));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
