package com.example.votacionessds.modules.auth.services.impl;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.votacionessds.exceptions.ErrorCode;
import com.example.votacionessds.exceptions.InvalidCredentialsException;
import com.example.votacionessds.exceptions.RefreshTokenReuseException;
import com.example.votacionessds.modules.auth.dao.UserRepository;
import com.example.votacionessds.modules.auth.dto.LoginRequest;
import com.example.votacionessds.modules.auth.dto.LoginResponse;
import com.example.votacionessds.modules.auth.dto.RefreshTokenResponse;
import com.example.votacionessds.modules.auth.entity.RefreshToken;
import com.example.votacionessds.modules.auth.entity.User;
import com.example.votacionessds.modules.auth.entity.UserSession;
import com.example.votacionessds.modules.auth.services.AuthService;
import com.example.votacionessds.modules.auth.services.LoginAuditService;
import com.example.votacionessds.security.CustomUserDetailsService;
import com.example.votacionessds.security.JwtProvider;
import com.example.votacionessds.security.RefreshTokenService;
import com.example.votacionessds.security.UserSessionService;
import com.example.votacionessds.security.RefreshTokenService.IssuedRefreshToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final LoginAuditService loginAuditService;
    private final UserSessionService userSessionService;
    private final CustomUserDetailsService userDetailsService;

    @Value("${security.login.max-failed-attempts:5}")
    private short maxFailedAttempts;

    @Value("${security.login.lock-duration-minutes:15}")
    private long lockDurationMinutes;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ip, String userAgent) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            // Nivel warn, no error: es una condición esperada (credenciales incorrectas), no una falla del sistema.
            log.warn("Login failed: email not found");
            loginAuditService.recordFailure(request.getEmail(), ip, userAgent);
            throw new InvalidCredentialsException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        }

        if (!user.isLoginAllowed()) {
            log.warn("Login failed: account locked or disabled. userId={}", user.getId());
            loginAuditService.recordFailure(user.getEmail(), ip, userAgent);
            throw new InvalidCredentialsException(ErrorCode.ACCOUNT_LOCKED, "Account is locked");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            user.registerSuccessfulLogin();
            userRepository.save(user);

            userSessionService.revokeAllActiveForUser(user.getId());
            refreshTokenService.revokeAllForUser(user);

            UserDetails principal = (UserDetails) authentication.getPrincipal();
            IssuedRefreshToken issued = refreshTokenService.issue(user, ip, userAgent);
            UserSession session = userSessionService.createSession(
                    user, issued.entity().getFamilyId(), ip, userAgent, issued.entity().getExpiresAt());
            String accessToken = jwtProvider.generateAccessToken(principal, session.getId());
            loginAuditService.recordSuccess(user, ip, userAgent);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(issued.rawToken())
                    .tokenType("Bearer")
                    .sessionId(session.getId())
                    .build();

        } catch (AuthenticationException ex) {
            // Cubre BadCredentials, Disabled, Locked, etc. — no solo BadCredentialsException.
            user.registerFailedLoginAttempt(maxFailedAttempts, Duration.ofMinutes(lockDurationMinutes));
            userRepository.save(user);
            loginAuditService.recordFailure(user.getEmail(), ip, userAgent);
            throw new InvalidCredentialsException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        }
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(String refreshToken, String ip, String userAgent) {
        IssuedRefreshToken rotated;
        try {
            rotated = refreshTokenService.rotate(refreshToken, ip, userAgent);
        } catch (RefreshTokenReuseException ex) {
            // Reuse confirmado: además de la familia de refresh tokens, se corta
            // también la UserSession -> el access token en manos del atacante deja
            // de ser válido de inmediato, sin esperar a que expire por su cuenta.
            userSessionService.revokeByFamilyId(ex.getFamilyId());
            throw new InvalidCredentialsException(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid refresh token");
        }

        User user = rotated.entity().getUser();
        userSessionService.touchByFamilyId(rotated.entity().getFamilyId());
        UUID sessionId = userSessionService.findActiveByFamilyId(rotated.entity().getFamilyId())
                .map(UserSession::getId)
                .orElseThrow(() -> new InvalidCredentialsException(
                        ErrorCode.SESSION_REVOKED, "Session is no longer active"));

        UserDetails principal = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtProvider.generateAccessToken(principal, sessionId);

        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rotated.rawToken())
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.resolve(refreshToken).ifPresent((RefreshToken token) -> {
            userSessionService.revokeByFamilyId(token.getFamilyId());
            refreshTokenService.revokeFamily(token.getFamilyId());
        });
    }
}
