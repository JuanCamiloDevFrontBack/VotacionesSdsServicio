package com.example.votacionessds.modules.auth.services;

import com.example.votacionessds.modules.auth.entity.RefreshToken;
import com.example.votacionessds.modules.auth.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenService {

    record IssuedRefreshToken(String rawToken, RefreshToken entity) {}

    IssuedRefreshToken issue(User user, String ip, String userAgent);

    Optional<RefreshToken> resolve(String rawToken);

    IssuedRefreshToken rotate(RefreshToken existing, String ip, String userAgent);

    void revoke(RefreshToken token);

    void revokeFamily(UUID familyId);

    void revokeAllForUser(User user);
}
