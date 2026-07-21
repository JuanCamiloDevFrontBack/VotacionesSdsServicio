package com.example.votacionessds.modules.auth.services;

import com.example.votacionessds.modules.auth.entity.RefreshToken;
import com.example.votacionessds.modules.auth.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenService {

    record IssuedRefreshToken(String rawToken, RefreshToken entity) {}

    IssuedRefreshToken issue(User user, String ip, String userAgent);

    /** Lectura simple, sin lock — usada por logout para localizar la familia a revocar. */
    Optional<RefreshToken> resolve(String rawToken);

    /**
     * Resuelve + valida + rota el token en una única transacción con lock pesimista.
     * Lanza RefreshTokenReuseException si el token ya estaba revocado (reuse).
     */
    IssuedRefreshToken rotate(String rawToken, String ip, String userAgent);

    void revokeFamily(UUID familyId);

    void revokeAllForUser(User user);
}
