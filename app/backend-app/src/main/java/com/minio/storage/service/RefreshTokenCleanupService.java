package com.minio.storage.service;

import com.minio.storage.repository.RefreshTokenRepository;
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
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            int deleted = refreshTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
            log.info("Obrisano {} isteklih refresh tokena.", deleted);
        } catch (Exception e) {
            log.error("Greška pri čišćenju refresh tokena: {}", e.getMessage());
        }
    }
}