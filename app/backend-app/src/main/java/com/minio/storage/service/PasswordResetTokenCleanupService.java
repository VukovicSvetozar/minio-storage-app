package com.minio.storage.service;

import com.minio.storage.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused")
public class PasswordResetTokenCleanupService {

    private final PasswordResetTokenRepository resetTokenRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            int deleted = resetTokenRepository.deleteByExpiryDateBeforeOrUsedTrue(LocalDateTime.now());
            log.info("Obrisano {} isteklih/iskorištenih password reset tokena.", deleted);
        } catch (Exception e) {
            log.error("Greška prilikom čišćenja isteklih tokena: {}", e.getMessage());
        }
    }

}