package com.example.votacionessds.modules.auth.services.impl;

import com.example.votacionessds.modules.auth.dao.RefreshTokenRepository;
import com.example.votacionessds.modules.auth.entity.RefreshToken;
import com.example.votacionessds.modules.auth.entity.User;
import com.example.votacionessds.modules.auth.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${security.refresh-token.expiration-time:604800000}")
    private long refreshTokenExpirationMs;

    @Override
    public RefreshToken createRefreshToken(User user) {
        RefreshToken rt = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(rt);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    public RefreshToken rotateRefreshToken(RefreshToken existing) {
        existing.setRevoked(true);
        RefreshToken rotated = createRefreshToken(existing.getUser());
        existing.setReplacedBy(rotated.getToken());
        refreshTokenRepository.save(existing);
        return rotated;
    }

    @Override
    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }
}
