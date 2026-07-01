package com.minio.storage.repository;

import com.minio.storage.entity.PasswordResetToken;
import com.minio.storage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findByUserAndUsedFalse(User user);

    void deleteByUser(User user);

    int deleteByExpiryDateBeforeOrUsedTrue(LocalDateTime now);

}