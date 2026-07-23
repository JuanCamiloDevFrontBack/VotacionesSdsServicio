package com.example.votacionessds.security;

public interface RefreshTokenHasher {

    String generateRawToken();

    /**
     * Hash determinístico (mismo input -> mismo output): necesario para poder
     * buscar el token por igualdad exacta en BD (WHERE token_hash = ?).
     * NUNCA loguear ni el rawToken ni el hash resultante — ver JwtAuthenticationFilter
     * y RefreshTokenServiceImpl para el manejo correcto de logs de seguridad.
     */
    String hash(String rawToken);
}
