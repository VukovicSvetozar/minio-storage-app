package com.minio.storage.repository;

import com.minio.storage.entity.RefreshToken;
import com.minio.storage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    void revokeAllByUser(User user);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < :now")
    int deleteByExpiryDateBefore(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.user = :user AND (t.revoked = true OR t.expiryDate < :now)")
    void deleteExpiredOrRevokedByUser(@Param("user") User user, @Param("now") LocalDateTime now);

}