package com.example.votacionessds.security;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.votacionessds.exceptions.ErrorCode;
import com.example.votacionessds.exceptions.InvalidCredentialsException;
import com.example.votacionessds.exceptions.RefreshTokenReuseException;
import com.example.votacionessds.modules.auth.dao.RefreshTokenRepository;
import com.example.votacionessds.modules.auth.entity.RefreshToken;
import com.example.votacionessds.modules.auth.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;

    @Value("${security.refresh-token.expiration-time:604800000}")
    private long refreshTokenExpirationMs;

    @Override
    @Transactional
    public IssuedRefreshToken issue(User user, String ip, String userAgent) {
        return persistToken(user, UUID.randomUUID(), ip, userAgent);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> resolve(String rawToken) {
        return refreshTokenRepository.findByTokenHash(refreshTokenHasher.hash(rawToken));
    }

    @Override
    @Transactional
    public IssuedRefreshToken rotate(String rawToken, String ip, String userAgent) {
        // Lock pesimista: bloquea la fila hasta el commit, cierra la ventana de
        // carrera que antes permitía que dos requests concurrentes con el mismo
        // token pasaran ambas la validación de "revoked = false".
        RefreshToken existing = refreshTokenRepository
                .findByTokenHashForUpdate(refreshTokenHasher.hash(rawToken))
                .orElseThrow(() -> new InvalidCredentialsException(
                        ErrorCode.INVALID_REFRESH_TOKEN,
                        "Invalid refresh token"));

        if (existing.isRevoked()) {
            revokeFamily(existing.getFamilyId());
            log.warn("SECURITY ALERT: refresh token reuse detected. userId={}, familyId={}",
                    existing.getUser().getId(), existing.getFamilyId());
            throw new RefreshTokenReuseException(existing.getFamilyId());
        }
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidCredentialsException(
                    ErrorCode.REFRESH_TOKEN_EXPIRED,
                    "Refresh token expired");
        }

        IssuedRefreshToken rotated = persistToken(existing.getUser(), existing.getFamilyId(), ip, userAgent);

        existing.setRevoked(true);
        existing.setRevokedAt(Instant.now());
        existing.setReplacedBy(rotated.entity().getId());
        refreshTokenRepository.save(existing);

        return rotated;
    }

    @Override
    @Transactional
    public void revokeFamily(UUID familyId) {
        List<RefreshToken> familyTokens = refreshTokenRepository.findByFamilyId(familyId);
        Instant now = Instant.now();
        for (RefreshToken token : familyTokens) {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                token.setRevokedAt(now);
            }
        }
        refreshTokenRepository.saveAll(familyTokens);
    }

    @Override
    @Transactional
    public void revokeAllForUser(User user) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
        Instant now = Instant.now();
        for (RefreshToken token : tokens) {
            token.setRevoked(true);
            token.setRevokedAt(now);
        }
        if (!tokens.isEmpty()) {
            refreshTokenRepository.saveAll(tokens);
        }
    }

    private IssuedRefreshToken persistToken(User user, UUID familyId, String ip, String userAgent) {
        String rawToken = refreshTokenHasher.generateRawToken();
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(refreshTokenHasher.hash(rawToken))
                .familyId(familyId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .userAgent(userAgent)
                .ipAddress(ip)
                .build();
        return new IssuedRefreshToken(rawToken, refreshTokenRepository.save(entity));
    }
}
