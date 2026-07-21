package com.example.votacionessds.modules.auth.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.votacionessds.modules.auth.entity.UserSession;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    List<UserSession> findByUserIdAndRevokedFalse(UUID userId);

    Optional<UserSession> findByFamilyId(UUID familyId);

    Optional<UserSession> findByFamilyIdAndRevokedFalse(UUID familyId);

    List<UserSession> findByRevokedFalseAndExpiresAtBefore(Instant expiresAt);
}
