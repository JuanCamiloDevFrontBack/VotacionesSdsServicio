package com.example.votacionessds.security;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.votacionessds.exceptions.ErrorCode;
import com.example.votacionessds.exceptions.InvalidCredentialsException;
import com.example.votacionessds.modules.auth.dao.RefreshTokenRepository;
import com.example.votacionessds.modules.auth.dao.UserSessionRepository;
import com.example.votacionessds.modules.auth.entity.User;
import com.example.votacionessds.modules.auth.entity.UserSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    private final UserSessionRepository userSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${security.retention.max-user-sessions:3}")
    private int maxUserSessions;

    @Override
    @Transactional
    public UserSession createSession(User user, UUID familyId, String ip, String userAgent, Instant expiresAt) {
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
        UserSession saved = userSessionRepository.save(session);
        pruneOlderSessions(user.getId());
        return saved;
    }

    @Override
    @Transactional
    public void touchByFamilyId(UUID familyId) {
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
        return userSessionRepository.findById(sessionId)
                .map(this::isSessionActive)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSession> findActiveByFamilyId(UUID familyId) {
        return userSessionRepository.findByFamilyIdAndRevokedFalse(familyId)
                .filter(this::isSessionActive);
    }

    @Override
    @Transactional
    public void revokeAllActiveForUser(UUID userId) {
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
        userSessionRepository.findByFamilyId(familyId).ifPresent(session -> {
            if (!session.isRevoked()) {
                revokeSessionInternal(session);
                log.warn("Session revoked by familyId: sessionId={}, familyId={}", session.getId(), familyId);
            }
        });
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void revokeExpiredSessions() {
        List<UserSession> expired = userSessionRepository.findByRevokedFalseAndExpiresAtBefore(Instant.now());
        for (UserSession session : expired) {
            revokeSessionInternal(session);
        }
        if (!expired.isEmpty()) {
            log.info("Expired sessions revoked automatically: count={}", expired.size());
        }
    }

    private void pruneOlderSessions(UUID userId) {
        if (maxUserSessions <= 0) {
            return;
        }
        List<UUID> excessFamilyIds = userSessionRepository.findFamilyIdsOlderThanKeep(userId, maxUserSessions);
        if (!excessFamilyIds.isEmpty()) {
            refreshTokenRepository.deleteByFamilyIdIn(excessFamilyIds);
        }
        int deleted = userSessionRepository.deleteOlderThanKeep(userId, maxUserSessions);
        if (deleted > 0) {
            log.info("Pruned older user sessions: userId={}, deleted={}", userId, deleted);
        }
    }

    private boolean isSessionActive(UserSession session) {
        return !session.isRevoked() && !session.getExpiresAt().isBefore(Instant.now());
    }

    private void revokeSessionInternal(UserSession session) {
        session.setRevoked(true);
        session.setRevokedAt(Instant.now());
        userSessionRepository.save(session);
    }
}
