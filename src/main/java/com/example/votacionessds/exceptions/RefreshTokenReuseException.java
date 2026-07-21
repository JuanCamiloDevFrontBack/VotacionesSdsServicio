package com.example.votacionessds.exceptions;

import java.util.UUID;

/**
 * Se lanza cuando un refresh token ya revocado/rotado se presenta de nuevo:
 * fuerte indicio de robo de token (OWASP A07). Carga el familyId para que
 * AuthServiceImpl pueda revocar también la UserSession asociada, sin crear
 * una dependencia circular entre RefreshTokenService y UserSessionService.
 */
public class RefreshTokenReuseException extends RuntimeException {

    private final UUID familyId;

    public RefreshTokenReuseException(UUID familyId) {
        super("Refresh token reuse detected");
        this.familyId = familyId;
    }

    public UUID getFamilyId() {
        return familyId;
    }
}
