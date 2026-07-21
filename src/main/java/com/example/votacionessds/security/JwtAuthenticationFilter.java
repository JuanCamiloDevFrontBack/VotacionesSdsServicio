package com.example.votacionessds.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.votacionessds.modules.auth.services.UserSessionService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * SIN @Component a propósito: esta clase se registra como @Bean explícito en
 * SecurityConfig. Si lleva @Component, Spring Boot la auto-registra ADEMÁS como
 * filtro de servlet genérico en "/*", duplicando su ejecución por request y
 * saltándose las reglas de authorizeHttpRequests.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "Authorization";
    private static final String HEADER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;
    private final UserSessionService userSessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER_NAME);
        String token = (StringUtils.hasText(header) && header.startsWith(HEADER_PREFIX))
                ? header.substring(HEADER_PREFIX.length())
                : null;

        if (token != null) {
            jwtProvider.parseValidClaims(token).ifPresent(claims -> authenticateIfSessionActive(claims, request));
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateIfSessionActive(JwtProvider.TokenClaims claims, HttpServletRequest request) {
        if (claims.sessionId() == null || !userSessionService.isActive(claims.sessionId())) {
            return; // token técnicamente válido, pero la sesión ya fue revocada/expiró
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(claims.username());

        // Defensa en profundidad: si el admin deshabilitó/bloqueó la cuenta DESPUÉS
        // de emitido el token, no se autentica aunque la sesión siga activa.
        if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
