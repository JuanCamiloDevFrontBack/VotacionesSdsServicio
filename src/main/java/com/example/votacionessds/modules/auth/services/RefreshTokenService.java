package com.example.votacionessds.modules.auth.services;

import com.example.votacionessds.modules.auth.entity.RefreshToken;
import com.example.votacionessds.modules.auth.entity.User;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);
    Optional<RefreshToken> findByToken(String token);
    RefreshToken rotateRefreshToken(RefreshToken existing);
    void revoke(RefreshToken token);
}
