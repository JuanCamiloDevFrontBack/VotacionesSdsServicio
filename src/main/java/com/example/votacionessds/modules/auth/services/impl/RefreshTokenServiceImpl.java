package com.example.votacionessds.modules.auth.services.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.votacionessds.exceptions.ErrorCode;
import com.example.votacionessds.exceptions.InvalidCredentialsException;
import com.example.votacionessds.modules.auth.dao.RefreshTokenRepository;
import com.example.votacionessds.modules.auth.entity.RefreshToken;
import com.example.votacionessds.modules.auth.entity.User;
import com.example.votacionessds.modules.auth.services.RefreshTokenService;
import com.example.votacionessds.security.RefreshTokenHasher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

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
        return refreshTokenRepository.findByTokenHash(RefreshTokenHasher.hash(rawToken));
    }

    @Override
    @Transactional
    public IssuedRefreshToken rotate(RefreshToken existing, String ip, String userAgent) {
        if (existing.isRevoked()) {
            revokeFamily(existing.getFamilyId());
            throw new InvalidCredentialsException(
                    ErrorCode.INVALID_REFRESH_TOKEN,
                    "Refresh token reuse detected");
        }
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidCredentialsException(
                    ErrorCode.REFRESH_TOKEN_EXPIRED,
                    "Refresh token expired");
        }

        IssuedRefreshToken rotated = persistToken(
                existing.getUser(),
                existing.getFamilyId(),
                ip,
                userAgent);

        existing.setRevoked(true);
        existing.setRevokedAt(Instant.now());
        existing.setReplacedBy(rotated.entity().getId());
        refreshTokenRepository.save(existing);

        return rotated;
    }

    @Override
    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
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
        System.out.println("--persistToken: --" + user);
        String rawToken = RefreshTokenHasher.generateRawToken();
        System.out.println("--rawToken: --" + rawToken);
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(RefreshTokenHasher.hash(rawToken))
                .familyId(familyId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .userAgent(userAgent)
                .ipAddress(ip)
                .build();
        System.out.println("--entity: --" + entity);
        return new IssuedRefreshToken(rawToken, refreshTokenRepository.save(entity));
    }
}
