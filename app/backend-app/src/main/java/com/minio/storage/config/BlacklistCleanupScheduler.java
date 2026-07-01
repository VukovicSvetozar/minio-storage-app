package com.minio.storage.config;

import com.minio.storage.service.BlacklistTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused")
public class BlacklistCleanupScheduler {

    private final BlacklistTokenService blacklistTokenService;

    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredTokens() {
        int removedCount = blacklistTokenService.cleanupExpiredTokens();
        if (removedCount > 0) {
            log.info("Očišćeno {} isteklih tokena iz blacklist-a", removedCount);
        }
    }

}