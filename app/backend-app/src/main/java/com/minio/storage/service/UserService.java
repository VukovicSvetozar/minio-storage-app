package com.minio.storage.service;

import com.minio.storage.dto.UserResponse;
import com.minio.storage.entity.User;
import com.minio.storage.enums.Role;
import com.minio.storage.exception.AppException;
import com.minio.storage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserSecurityService userSecurityService;
    private final ShareLinkService shareLinkService;
    private final UserRepository userRepository;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll(Sort.by("id").descending())
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Korisnik sa ID-em " + id + " nije pronađen."));
        return convertToResponse(user);
    }

    @Transactional
    public UserResponse updateUserRole(Long id, String roleString) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound(
                        "Korisnik sa ID-em " + id + " nije pronađen."));

        if ("system".equals(user.getUsername())) {
            throw AppException.forbidden(
                    "Sistemski administratorski nalog nije moguće mijenjati.");
        }

        User currentUser = userSecurityService.getCurrentUser();
        if (user.getId().equals(currentUser.getId())) {
            throw AppException.badRequest("Ne možete mijenjati sopstvenu ulogu.");
        }

        Role newRole;
        try {
            newRole = Role.valueOf(roleString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest(
                    "Neispravna uloga: " + roleString + ". Dozvoljene vrijednosti su USER i ADMIN.");
        }

        if (user.getRole() == newRole) {
            throw AppException.badRequest(
                    "Korisnik već posjeduje dodijeljenu ulogu: " + newRole);
        }

        if (user.getRole() == Role.ADMIN && newRole == Role.USER) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw AppException.badRequest(
                        "Nije moguće ukloniti ulogu - sistem mora imati bar jednog administratora.");
            }
        }

        Role oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);

        log.info("Promjena role za korisnika {}: {} -> {}",
                user.getUsername(), oldRole, newRole);
        return convertToResponse(user);
    }

    @Transactional
    public UserResponse toggleUserStatus(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound(
                        "Korisnik sa ID-em " + id + " nije pronađen."));

        if ("system".equals(user.getUsername())) {
            throw AppException.forbidden(
                    "Sistemski administratorski nalog nije moguće deaktivirati.");
        }

        User currentUser = userSecurityService.getCurrentUser();

        if (user.getId().equals(currentUser.getId())) {
            throw AppException.badRequest(
                    "Ne možete deaktivirati sopstveni nalog.");
        }

        if (user.getRole() == Role.ADMIN && user.isActive()) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw AppException.badRequest(
                        "Nije moguće deaktivirati korisnika - sistem mora imati bar jednog administratora.");
            }
        }

        boolean oldStatus = user.isActive();
        user.setActive(!oldStatus);
        userRepository.save(user);

        log.info("Promjena statusa korisnika {}: {} -> {}",
                user.getUsername(), oldStatus, user.isActive());

        return convertToResponse(user);

    }

    @Transactional
    public UserResponse deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound(
                        "Korisnik sa ID-em " + id + " nije pronađen."));

        if ("system".equals(user.getUsername())) {
            throw AppException.forbidden("Sistemski nalog ne može biti deaktiviran.");
        }

        User currentUser = userSecurityService.getCurrentUser();
        if (user.getId().equals(currentUser.getId())) {
            throw AppException.badRequest("Ne možete deaktivirati sopstveni nalog.");
        }

        if (user.getRole() == Role.ADMIN && user.isActive()) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw AppException.badRequest(
                        "Sistem mora imati bar jednog aktivnog administratora.");
            }
        }

        if (!user.isActive()) {
            log.info("Korisnik {} je već deaktiviran.", user.getUsername());
            return convertToResponse(user);
        }

        shareLinkService.deactivateShareLinksByUsername(user.getUsername());

        user.setActive(false);
        userRepository.save(user);

        log.info("Korisnički nalog deaktiviran: username={}", user.getUsername());
        return convertToResponse(user);
    }

    private UserResponse convertToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isActive(user.isActive())
                .build();
    }

}