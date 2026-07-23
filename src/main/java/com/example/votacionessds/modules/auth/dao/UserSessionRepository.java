package com.example.votacionessds.modules.auth.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< Updated upstream
=======
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
>>>>>>> Stashed changes
import org.springframework.stereotype.Repository;

import com.example.votacionessds.modules.auth.entity.UserSession;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    List<UserSession> findByUserIdAndRevokedFalse(UUID userId);

    Optional<UserSession> findByFamilyId(UUID familyId);

    Optional<UserSession> findByFamilyIdAndRevokedFalse(UUID familyId);

    List<UserSession> findByRevokedFalseAndExpiresAtBefore(Instant expiresAt);
<<<<<<< Updated upstream
=======

    @Query(value = """
            SELECT family_id FROM user_sessions
            WHERE user_id = :userId
              AND id NOT IN (
                SELECT id FROM (
                  SELECT id FROM user_sessions
                  WHERE user_id = :userId
                  ORDER BY created_at DESC, id DESC
                  LIMIT :keep
                ) keep_rows
              )
            """, nativeQuery = true)
    List<UUID> findFamilyIdsOlderThanKeep(@Param("userId") UUID userId, @Param("keep") int keep);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            DELETE FROM user_sessions us
            WHERE us.user_id = :userId
              AND us.id NOT IN (
                SELECT id FROM (
                  SELECT id FROM user_sessions
                  WHERE user_id = :userId
                  ORDER BY created_at DESC, id DESC
                  LIMIT :keep
                ) keep_rows
              )
            """, nativeQuery = true)
    int deleteOlderThanKeep(@Param("userId") UUID userId, @Param("keep") int keep);
>>>>>>> Stashed changes
}
