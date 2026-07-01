package com.minio.storage.service;

import com.minio.storage.entity.User;
import com.minio.storage.enums.Role;
import com.minio.storage.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSecurityService {

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw AppException.unauthorized("Korisnik nije autentifikovan");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return user;
        }

        throw AppException.internalError("Principal nije instanca User klase");
    }

    public User getCurrentUserOrNull() {
        try {
            return getCurrentUser();
        } catch (AppException e) {
            if (e.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
                log.error("Kritična greška pri parsiranju Security konteksta", e);
                throw e;
            }
            return null;
        }
    }

    public boolean isCurrentUserAdmin() {
        User user = getCurrentUser();
        return user.getRole() == Role.ADMIN;
    }

}
