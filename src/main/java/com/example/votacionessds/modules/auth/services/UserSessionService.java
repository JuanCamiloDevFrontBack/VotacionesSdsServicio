package com.example.votacionessds.modules.auth.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.votacionessds.modules.auth.dto.SessionResponse;
import com.example.votacionessds.modules.auth.entity.User;
import com.example.votacionessds.modules.auth.entity.UserSession;

public interface UserSessionService {

    UserSession createSession(User user, UUID familyId, String ip, String userAgent, Instant expiresAt);

    void touchByFamilyId(UUID familyId);

    boolean isActive(UUID sessionId);

    Optional<UserSession> findActiveByFamilyId(UUID familyId);

    void revokeSession(UUID sessionId, UUID userId);

    void revokeAllActiveForUser(UUID userId);

    void revokeByFamilyId(UUID familyId);

    List<SessionResponse> listActiveForUser(UUID userId, UUID currentSessionId);
}
