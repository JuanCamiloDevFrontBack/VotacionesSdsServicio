package com.example.votacionessds.modules.auth.services.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.votacionessds.exceptions.ErrorCode;
import com.example.votacionessds.exceptions.InvalidCredentialsException;
import com.example.votacionessds.exceptions.ResourceNotFoundException;
import com.example.votacionessds.modules.auth.dao.UserSessionRepository;
import com.example.votacionessds.modules.auth.dto.SessionResponse;
import com.example.votacionessds.modules.auth.entity.User;
import com.example.votacionessds.modules.auth.entity.UserSession;
import com.example.votacionessds.modules.auth.services.RefreshTokenService;
import com.example.votacionessds.modules.auth.services.UserSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    private final UserSessionRepository userSessionRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public UserSession createSession(User user, UUID familyId, String ip, String userAgent, Instant expiresAt) {
        System.out.println("UserSessionServiceImpl: createSession");
        Instant now = Instant.now();
        UserSession session = UserSession.builder()
                .user(user)
                .familyId(familyId)
                .ipAddress(ip)
                .userAgent(userAgent)
                .createdAt(now)
                .lastSeenAt(now)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        return userSessionRepository.save(session);
    }

    @Override
    @Transactional
    public void touchByFamilyId(UUID familyId) {
        System.out.println("UserSessionServiceImpl: touchByFamilyId");
        UserSession session = userSessionRepository.findByFamilyIdAndRevokedFalse(familyId)
                .orElseThrow(() -> new InvalidCredentialsException(
                        ErrorCode.SESSION_REVOKED,
                        "Session is no longer active"));
        if (session.getExpiresAt().isBefore(Instant.now())) {
            revokeSessionInternal(session);
            throw new InvalidCredentialsException(
                    ErrorCode.SESSION_EXPIRED,
                    "Session has expired");
        }
        session.setLastSeenAt(Instant.now());
        userSessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(UUID sessionId) {
        System.out.println("UserSessionServiceImpl: isActive");
        return userSessionRepository.findById(sessionId)
                .map(this::isSessionActive)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSession> findActiveByFamilyId(UUID familyId) {
        System.out.println("UserSessionServiceImpl: findActiveByFamilyId");
        return userSessionRepository.findByFamilyIdAndRevokedFalse(familyId)
                .filter(this::isSessionActive);
    }

    @Override
    @Transactional
    public void revokeSession(UUID sessionId, UUID userId) {
        System.out.println("UserSessionServiceImpl: revokeSession");
        UserSession session = userSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.SESSION_NOT_FOUND,
                        "Session not found"));
        if (!session.isRevoked()) {
            revokeSessionInternal(session);
            refreshTokenService.revokeFamily(session.getFamilyId());
            log.warn("Session revoked: sessionId={}, userId={}", sessionId, userId);
        }
    }

    @Override
    @Transactional
    public void revokeAllActiveForUser(UUID userId) {
        System.out.println("UserSessionServiceImpl: revokeAllActiveForUser");
        List<UserSession> sessions = userSessionRepository.findByUserIdAndRevokedFalse(userId);
        Instant now = Instant.now();
        for (UserSession session : sessions) {
            session.setRevoked(true);
            session.setRevokedAt(now);
        }
        if (!sessions.isEmpty()) {
            userSessionRepository.saveAll(sessions);
            log.warn("All active sessions revoked for userId={}, count={}", userId, sessions.size());
        }
    }

    @Override
    @Transactional
    public void revokeByFamilyId(UUID familyId) {
        System.out.println("UserSessionServiceImpl: revokeByFamilyId");
        userSessionRepository.findByFamilyId(familyId).ifPresent(session -> {
            if (!session.isRevoked()) {
                revokeSessionInternal(session);
                log.warn("Session revoked by familyId: sessionId={}, familyId={}", session.getId(), familyId);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> listActiveForUser(UUID userId, UUID currentSessionId) {
        System.out.println("UserSessionServiceImpl: listActiveForUser");
        return userSessionRepository.findByUserIdAndRevokedFalse(userId).stream()
                .filter(this::isSessionActive)
                .map(session -> toSessionResponse(session, currentSessionId))
                .toList();
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void revokeExpiredSessions() {
        System.out.println("UserSessionServiceImpl: revokeExpiredSessions");
        List<UserSession> expired = userSessionRepository.findByRevokedFalseAndExpiresAtBefore(Instant.now());
        for (UserSession session : expired) {
            revokeSessionInternal(session);
        }
        if (!expired.isEmpty()) {
            log.info("Expired sessions revoked automatically: count={}", expired.size());
        }
    }

    private boolean isSessionActive(UserSession session) {
        System.out.println("UserSessionServiceImpl: isSessionActive");
        return !session.isRevoked() && !session.getExpiresAt().isBefore(Instant.now());
    }

    private void revokeSessionInternal(UserSession session) {
        System.out.println("UserSessionServiceImpl: revokeSessionInternal");
        session.setRevoked(true);
        session.setRevokedAt(Instant.now());
        userSessionRepository.save(session);
    }

    private SessionResponse toSessionResponse(UserSession session, UUID currentSessionId) {
        System.out.println("UserSessionServiceImpl: toSessionResponse");
        return SessionResponse.builder()
                .id(session.getId())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .createdAt(session.getCreatedAt())
                .lastSeenAt(session.getLastSeenAt())
                .expiresAt(session.getExpiresAt())
                .current(currentSessionId != null && currentSessionId.equals(session.getId()))
                .build();
    }
}
