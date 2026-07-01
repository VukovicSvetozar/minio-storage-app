package com.minio.storage.service;

import com.minio.storage.entity.Bucket;
import com.minio.storage.entity.FileMetadata;
import com.minio.storage.entity.User;
import com.minio.storage.enums.FileAction;
import com.minio.storage.exception.AppException;
import com.minio.storage.repository.BucketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileSecurityService {

    private final UserSecurityService userSecurityService;
    private final BucketRepository bucketRepository;

    public void checkCanView(FileMetadata file) {
        if (file.isPublic()) return;

        Bucket bucket = bucketRepository
                .findByNameAndDeletedAtIsNull(file.getBucketName())
                .orElseThrow(() -> AppException.notFound("Bucket nije pronađen."));

        if (bucket.isPublic()) return;

        checkAdminOrOwner(file, FileAction.VIEW);
    }

    public void checkCanUpload(String bucketName) {
        Bucket bucket = bucketRepository
                .findByNameAndDeletedAtIsNull(bucketName)
                .orElseThrow(() -> AppException.notFound("Bucket nije pronađen."));

        User current = userSecurityService.getCurrentUserOrNull();
        if (current == null) throw AppException.forbidden(getMessage(FileAction.UPLOAD));

        if (userSecurityService.isCurrentUserAdmin()) return;
        if (isOwner(bucket.getOwner(), current)) return;
        if (bucket.isAllowPublicUpload()) return;

        log.warn("Neovlašćen pokušaj otpremanja: bucket={}, korisnik={}",
                bucketName, current.getEmail());
        throw AppException.forbidden(getMessage(FileAction.UPLOAD));
    }

    public void checkCanModify(FileMetadata file) {
        checkAdminOrOwner(file, FileAction.MODIFY);
    }

    public void checkCanDelete(FileMetadata file) {
        checkAdminOrOwner(file, FileAction.DELETE);
    }

    public void checkCanAccessVersions(FileMetadata file) {
        checkAdminOrOwner(file, FileAction.VIEW);
    }

    public void checkCanShare(FileMetadata file) {
        User current = userSecurityService.getCurrentUserOrNull();

        if (current == null) {
            throw AppException.forbidden("Nemate pravo dijeljenja ovog fajla.");
        }

        if (userSecurityService.isCurrentUserAdmin()) return;

        if (isOwner(file.getOwner(), current)) return;

        log.warn("Neovlašćen pokušaj dijeljenja fajla: id={}, korisnik={}",
                file.getId(), current.getEmail());

        throw AppException.forbidden("Nemate pravo dijeljenja ovog fajla.");
    }

    private void checkAdminOrOwner(FileMetadata file, FileAction action) {
        User current = requireUser(action);

        if (userSecurityService.isCurrentUserAdmin()) return;
        if (isOwner(file.getOwner(), current)) return;

        Bucket bucket = bucketRepository
                .findByNameAndDeletedAtIsNull(file.getBucketName())
                .orElseThrow(() -> AppException.notFound("Bucket nije pronađen."));

        if (isOwner(bucket.getOwner(), current)) return;

        log.warn("Neovlašćen pokušaj {} fajla: id={}, korisnik={}",
                action.name().toLowerCase(), file.getId(), current.getEmail());
        throw AppException.forbidden(getMessage(action));
    }

    private User requireUser(FileAction action) {
        User current = userSecurityService.getCurrentUserOrNull();
        if (current == null) throw AppException.forbidden(getMessage(action));
        return current;
    }

    private String getMessage(FileAction action) {
        return switch (action) {
            case VIEW -> "Nemate pravo pristupa ovom fajlu.";
            case MODIFY -> "Nemate pravo izmjene ovog fajla.";
            case DELETE -> "Nemate pravo brisanja ovog fajla.";
            case UPLOAD -> "Nemate pravo otpremanja u ovaj bucket.";
            case SHARE -> "Nemate pravo dijeljenja ovog fajla.";
        };
    }

    private boolean isOwner(User owner, User current) {
        return owner != null && owner.getId().equals(current.getId());
    }

}