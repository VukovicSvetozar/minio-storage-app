package com.minio.storage.service;

import com.minio.storage.entity.RefreshToken;
import com.minio.storage.entity.User;
import com.minio.storage.exception.AppException;
import com.minio.storage.repository.RefreshTokenRepository;
import com.minio.storage.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtUtil jwtUtil;
    private final Long refreshExpiration;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            JwtUtil jwtUtil,
            @Value("${jwt.refresh-expiration}") Long refreshExpiration) {

        this.repository = repository;
        this.jwtUtil = jwtUtil;
        this.refreshExpiration = refreshExpiration;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        repository.deleteExpiredOrRevokedByUser(user, LocalDateTime.now());

        String tokenValue = jwtUtil.generateRefreshToken(user);

        RefreshToken token = RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();

        return repository.save(token);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String oldTokenValue) {
        if (!jwtUtil.isTokenValid(oldTokenValue)) {
            throw AppException.unauthorized("Nevažeći format ili potpis refresh tokena", null);
        }

        String tokenType = jwtUtil.extractTokenType(oldTokenValue);
        if (!"refresh".equals(tokenType)) {
            throw AppException.unauthorized("Proslijeđen je pogrešan tip tokena (očekivan refresh)", null);
        }

        RefreshToken oldToken = repository.findByToken(oldTokenValue)
                .orElseThrow(() -> AppException.unauthorized("Refresh token nije pronađen u bazi", null));

        if (oldToken.isRevoked()) {
            log.error("Replay napad! Token je već korišten: {}", oldTokenValue);
            revokeAllUserTokens(oldToken.getUser());
            throw AppException.unauthorized("Token je već korišten. Sve sesije su opozvane.", null);
        }

        if (!oldToken.isValid()) {
            throw AppException.unauthorized("Refresh token je istekao", null);
        }

        RefreshToken newToken = createRefreshToken(oldToken.getUser());
        oldToken.revoke();
        repository.save(oldToken);

        log.info("Refresh token rotiran za korisnika: {}", oldToken.getUser().getUsername());
        return newToken;
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        repository.revokeAllByUser(user);
        log.info("Sve sesije opozvane za korisnika: {}", user.getUsername());
    }

}