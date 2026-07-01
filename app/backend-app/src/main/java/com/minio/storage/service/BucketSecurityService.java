package com.minio.storage.service;

import com.minio.storage.entity.Bucket;
import com.minio.storage.enums.BucketAction;
import com.minio.storage.entity.User;
import com.minio.storage.exception.AppException;
import com.minio.storage.repository.BucketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BucketSecurityService {

    private final UserSecurityService userSecurityService;
    private final BucketRepository bucketRepository;

    public void checkCanViewBucket(String bucketName) {
        Bucket bucket = bucketRepository
                .findByNameAndDeletedAtIsNull(bucketName)
                .orElseThrow(() -> AppException.notFound("Bucket nije pronađen."));
        checkCanView(bucket);
    }

    public void checkCanView(Bucket bucket) {
        if (bucket.isPublic()) return;
        checkAdminOrOwner(bucket, BucketAction.VIEW);
    }

    public void checkCanUpdate(Bucket bucket) {
        checkAdminOrOwner(bucket, BucketAction.UPDATE);
    }

    public void checkCanDelete(Bucket bucket) {
        checkAdminOrOwner(bucket, BucketAction.DELETE);
    }

    public void checkCanViewVersioning(Bucket bucket) {
        checkAdminOrOwner(bucket, BucketAction.VIEW);
    }

    private void checkAdminOrOwner(Bucket bucket, BucketAction action) {

        User current = userSecurityService.getCurrentUserOrNull();

        if (current == null) {
            throw AppException.forbidden(getMessage(action));
        }

        if (userSecurityService.isCurrentUserAdmin()) return;

        if (isOwner(bucket, current)) return;

        log.warn("Neovlašćen pokušaj {} nad bucketom '{}': korisnik '{}'",
                action, bucket.getName(), current.getEmail());

        throw AppException.forbidden(getMessage(action));
    }

    private String getMessage(BucketAction action) {
        return switch (action) {
            case VIEW -> "Nemate pravo pristupa ovom bucketu.";
            case UPDATE -> "Nemate pravo izmjene ovog bucketa.";
            case DELETE -> "Nemate pravo brisanja ovog bucketa.";
        };
    }

    private boolean isOwner(Bucket bucket, User user) {
        return bucket.getOwner() != null &&
                bucket.getOwner().getId().equals(user.getId());
    }

}