package com.example.votacionessds.modules.auth.dao;

<<<<<<< Updated upstream
<<<<<<< Updated upstream
import com.example.votacionessds.modules.auth.entity.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {
=======
=======
>>>>>>> Stashed changes
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.votacionessds.modules.auth.entity.LoginAudit;

@Repository
public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {

    @Modifying(clearAutomatically = true)
    @Query(value = """
            DELETE FROM login_audit la
            WHERE la.user_id = :userId
              AND la.id NOT IN (
                SELECT id FROM (
                  SELECT id FROM login_audit
                  WHERE user_id = :userId
                  ORDER BY created_at DESC, id DESC
                  LIMIT :keep
                ) keep_rows
              )
            """, nativeQuery = true)
    int deleteOlderThanKeepByUserId(@Param("userId") UUID userId, @Param("keep") int keep);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            DELETE FROM login_audit la
            WHERE la.user_id IS NULL
              AND la.email_attempted = :email
              AND la.id NOT IN (
                SELECT id FROM (
                  SELECT id FROM login_audit
                  WHERE user_id IS NULL
                    AND email_attempted = :email
                  ORDER BY created_at DESC, id DESC
                  LIMIT :keep
                ) keep_rows
              )
            """, nativeQuery = true)
    int deleteOlderThanKeepByEmailWithoutUser(@Param("email") String email, @Param("keep") int keep);
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes
}
