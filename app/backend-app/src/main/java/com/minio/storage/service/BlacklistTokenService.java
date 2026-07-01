package com.minio.storage.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@SuppressWarnings("unused")
public class BlacklistTokenService {

    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token, long expirationTimeMs) {
        blacklistedTokens.put(token, expirationTimeMs);
    }

    public boolean isTokenBlacklisted(String token) {
        if (!blacklistedTokens.containsKey(token)) {
            return false;
        }

        long expirationTime = blacklistedTokens.get(token);
        long currentTime = System.currentTimeMillis();

        if (currentTime > expirationTime) {
            blacklistedTokens.remove(token);
            return false;
        }

        return true;
    }

    public int cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        int initialSize = blacklistedTokens.size();
        blacklistedTokens.values().removeIf(expiry -> currentTime > expiry);

        return initialSize - blacklistedTokens.size();
    }

    public int getBlacklistedTokenCount() {
        return blacklistedTokens.size();
    }

}