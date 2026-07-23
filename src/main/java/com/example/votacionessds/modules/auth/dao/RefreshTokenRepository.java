package com.example.votacionessds.modules.auth.dao;

<<<<<<< Updated upstream
=======
import java.util.Collection;
>>>>>>> Stashed changes
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
<<<<<<< Updated upstream
import org.springframework.data.jpa.repository.Query;
=======
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
>>>>>>> Stashed changes
import org.springframework.stereotype.Repository;

import com.example.votacionessds.modules.auth.entity.RefreshToken;
import com.example.votacionessds.modules.auth.entity.User;

import jakarta.persistence.LockModeType;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Lock pesimista de fila: usado SOLO en la rotación (RefreshTokenServiceImpl.rotate).
     * Si dos requests llegan con el mismo refresh token casi simultáneas, la segunda
     * espera a que la primera confirme su transacción antes de leer el estado —
     * elimina la ventana de carrera que permitía saltarse la detección de reuse.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshToken r where r.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    List<RefreshToken> findByFamilyId(UUID familyId);

    List<RefreshToken> findByUserAndRevokedFalse(User user);
<<<<<<< Updated upstream
=======

    @Modifying(clearAutomatically = true)
    @Query("delete from RefreshToken r where r.familyId in :familyIds")
    int deleteByFamilyIdIn(@Param("familyIds") Collection<UUID> familyIds);
>>>>>>> Stashed changes
}
