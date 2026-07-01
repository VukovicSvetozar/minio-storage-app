package com.minio.storage.service;

import com.minio.storage.dto.*;
import com.minio.storage.entity.Bucket;
import com.minio.storage.entity.User;
import com.minio.storage.enums.BucketSyncStatus;
import com.minio.storage.enums.MinioReachabilityStatus;
import com.minio.storage.exception.AppException;
import com.minio.storage.repository.BucketRepository;
import com.minio.storage.repository.FileMetadataRepository;
import com.minio.storage.repository.UserRepository;
import com.minio.storage.util.ActivityLogHelper;
import com.minio.storage.util.FormatUtils;
import com.minio.storage.util.TxCleanup;
import io.minio.messages.VersioningConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class BucketService {

    private final UserSecurityService userSecurityService;
    private final MinioService minioService;
    private final BucketSecurityService bucketSecurityService;
    private final BucketSyncWorker bucketSyncWorker;
    private final UserRepository userRepository;
    private final BucketRepository bucketRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final ActivityLogHelper activityLogHelper;

    @Value("${app.bucket.max-buckets-per-user:10}")
    private int maxBucketsPerUser;

    @Value("${app.bucket.default-quota-bytes:10737418240}")
    private Long defaultQuotaBytes;

    @Transactional(readOnly = true)
    public boolean isNameAvailable(String name) {
        String normalized = normalizeBucketName(name);
        if (bucketRepository.existsByName(normalized)) {
            return false;
        }
        return !minioService.bucketExists(normalized);
    }

    @Transactional
    public BucketResponse createBucket(CreateBucketRequest request) {

        String name = normalizeBucketName(request.getBucketName());

        if (!isNameAvailable(name)) {
            throw AppException.conflict("Naziv '" + name + "' je već zauzet.");
        }

        User currentUser = userSecurityService.getCurrentUser();
        checkBucketLimit(currentUser);

        Bucket bucket = new Bucket(
                name,
                currentUser,
                request.isPublic(),
                request.getMaxSizeGb() * 1024L * 1024L * 1024L
        );

        bucket.setAllowPublicUpload(request.isAllowPublicUpload());
        bucket.setDescription(request.getDescription());
        bucket.setObjectLockEnabled(request.isObjectLock());

        minioService.createBucket(name, request.isObjectLock());

        TxCleanup.onRollback(() -> minioService.deleteBucket(name));

        if (bucket.isPublic()) {
            minioService.setBucketPolicy(name, true);
        }

        Bucket saved = bucketRepository.save(bucket);

        return convertToResponse(saved);
    }

    @Transactional
    public void deleteBucket(String name) {
        String normalized = normalizeBucketName(name);
        Bucket bucket = findActiveBucketByName(normalized);

        bucketSecurityService.checkCanDelete(bucket);

        if (bucket.getFileCount() > 0) {
            throw AppException.conflict(
                    "Bucket nije prazan. Sadrži " + bucket.getFileCount() +
                            " fajl(ova). Potrebno je prvo obrisati sve fajlove."
            );
        }

        minioService.deleteBucket(bucket.getName());

        boolean wasObjectLock = bucket.isObjectLockEnabled();
        TxCleanup.onRollback(() -> {
            try {
                minioService.createBucket(bucket.getName(), wasObjectLock);
                if (bucket.isPublic()) {
                    minioService.setBucketPolicy(bucket.getName(), true);
                }
                log.info("MinIO: Rollback uspješan – bucket '{}' je ponovo kreiran (sinhronizacija sa bazom)", normalized);
            } catch (Exception e) {
                log.error("KRITIČNO: Rollback cleanup nije uspio! Bucket '{}' ostao obrisan na MinIO serveru iako transakcija nije uspjela", normalized, e);
            }
        });

        bucket.setDeletedAt(LocalDateTime.now());
        bucketRepository.save(bucket);

    }

    @Transactional
    public BucketResponse updateBucketPolicy(String name, UpdateBucketPolicyRequest request) {

        String normalized = normalizeBucketName(name);
        Bucket bucket = findActiveBucketByName(normalized);

        bucketSecurityService.checkCanUpdate(bucket);

        boolean newPublic = request.getIsPublic();
        Boolean requestedUpload = request.getAllowPublicUpload();

        boolean finalUpload;

        if (!newPublic) {
            finalUpload = false;
        } else finalUpload = Objects.requireNonNullElseGet(requestedUpload, bucket::isAllowPublicUpload);

        if (bucket.isPublic() == newPublic &&
                bucket.isAllowPublicUpload() == finalUpload) {

            return convertToResponse(bucket);
        }

        boolean originalPublicState = bucket.isPublic();
        minioService.setBucketPolicy(bucket.getName(), newPublic);

        TxCleanup.onRollback(() -> minioService.setBucketPolicy(bucket.getName(), originalPublicState));

        bucket.setPublic(newPublic);
        bucket.setAllowPublicUpload(finalUpload);
        Bucket saved = bucketRepository.save(bucket);

        log.info("Bucket '{}' polisa ažurirana na {}",
                normalized,
                saved.isPublic() ? "PUBLIC" : "PRIVATE");

        return convertToResponse(saved);
    }

    @Transactional(readOnly = true)
    public BucketResponse getBucketById(Long id) {
        Bucket bucket = findActiveBucketById(id);
        bucketSecurityService.checkCanView(bucket);
        return convertToResponse(bucket);
    }

    @Transactional(readOnly = true)
    public BucketResponse getBucketByName(String name) {
        String normalized = normalizeBucketName(name);
        Bucket bucket = findActiveBucketByName(normalized);
        bucketSecurityService.checkCanView(bucket);
        return convertToResponse(bucket);
    }

    @Transactional(readOnly = true)
    public List<BucketResponse> getAllBuckets() {
        List<Bucket> buckets = bucketRepository.findAllByDeletedAtIsNull();
        return convertToResponseList(buckets);

    }

    @Transactional(readOnly = true)
    public List<BucketResponse> getMyBuckets() {
        User current = userSecurityService.getCurrentUser();
        List<Bucket> buckets = bucketRepository
                .findByOwnerIdAndDeletedAtIsNull(current.getId());
        return convertToResponseList(buckets);
    }

    @Transactional(readOnly = true)
    public List<BucketResponse> getPublicBuckets() {
        List<Bucket> buckets = bucketRepository
                .findByIsPublicTrueAndDeletedAtIsNull();
        return convertToResponseList(buckets);
    }

    @Transactional(readOnly = true)
    public VersioningStatusResponse getVersioningStatus(String bucketName) {

        String normalized = normalizeBucketName(bucketName);
        Bucket bucket = findActiveBucketByName(normalized);

        bucketSecurityService.checkCanViewVersioning(bucket);

        VersioningConfiguration.Status status = minioService.getBucketVersioningStatus(normalized);
        boolean isEnabled = (status == VersioningConfiguration.Status.ENABLED);

        return VersioningStatusResponse.builder()
                .bucketName(normalized)
                .versioningEnabled(isEnabled)
                .status(status.name())
                .build();
    }

    @Transactional
    public BucketResponse updateBucketVersioning(String name, VersioningUpdateBucketRequest request, HttpServletRequest httpRequest) {
        String normalized = normalizeBucketName(name);

        Bucket bucket = bucketRepository.findByNameForUpdate(normalized)
                .orElseThrow(() -> AppException.notFound("Bucket nije pronađen: " + normalized));

        boolean oldStatus = bucket.isVersioningEnabled();
        boolean newStatus = request.isEnabled();

        if (oldStatus == newStatus) {
            return convertToResponse(bucket);
        }
        minioService.setBucketVersioning(normalized, newStatus);

        TxCleanup.onRollback(() -> {
            try {
                minioService.setBucketVersioning(normalized, oldStatus);
                log.info("Rollback: vraćen versioning za '{}' na {}", normalized, oldStatus);
            } catch (Exception e) {
                log.error("Rollback FAILED za versioning '{}'", normalized, e);
            }
        });

        bucket.setVersioningEnabled(newStatus);
        Bucket saved = bucketRepository.save(bucket);

        try {
            String poruka = "Verzioniranje za bucket '" + normalized + "' postavljeno na: " + (newStatus ? "ENABLED" : "SUSPENDED");
            activityLogHelper.logBucketUpdate(saved.getName(), saved.getId(), poruka, httpRequest);
        } catch (Exception e) {
            log.warn("Logovanje aktivnosti nije uspjelo", e);
        }

        return convertToResponse(saved);
    }

    public BucketSyncResponse syncBucketsFromMinIO() {

        User systemUser = userRepository.findByUsername("system")
                .orElseThrow(() -> AppException.internalError(
                        "Sistemski nalog 'system' nije pronađen."));

        List<String> minioBuckets = minioService.listAllBuckets();

        int imported = 0, updated = 0, skipped = 0, errors = 0;
        List<BucketSyncResult> results = new ArrayList<>();

        for (String name : minioBuckets) {
            try {
                BucketSyncResult result = bucketSyncWorker.syncSingleBucket(name, systemUser);
                results.add(result);

                switch (result.getStatus()) {
                    case IMPORTED -> imported++;
                    case UPDATED -> updated++;
                    case SKIPPED -> skipped++;
                    case ERROR -> errors++;
                }
            } catch (Exception e) {
                log.error("SYNC: Greška pri obradi bucketa '{}': {}", name, e.getMessage());
                results.add(new BucketSyncResult(name, BucketSyncStatus.ERROR, null, null, false));
                errors++;
            }
        }

        log.info("Sinhronizacija završena → Uvezeno: {}, Ažurirano: {}, Preskočeno: {}",
                imported, updated, skipped);

        return new BucketSyncResponse(imported, updated, skipped, errors, results);
    }

    @Transactional
    public BucketResponse recalculateBucketStatistics(String bucketName) {
        String normalized = normalizeBucketName(bucketName);
        Bucket bucket = bucketRepository.findByNameAndDeletedAtIsNull(normalized)
                .orElseThrow(() -> AppException.notFound("Bucket nije pronađen."));

        Object[] stats = fileMetadataRepository.getBucketStatistics(normalized);

        if (stats != null && stats.length >= 2) {
            long fileCount = stats[0] != null ? ((Number) stats[0]).longValue() : 0L;
            long totalSize = stats[1] != null ? ((Number) stats[1]).longValue() : 0L;
            bucket.syncStatistics(fileCount, totalSize);
        } else {
            bucket.syncStatistics(0L, 0L);
        }

        bucketRepository.save(bucket);

        log.warn("Statistika bucketa ručno osvježena: bucket={}, fajlova={}, veličina={}",
                bucketName, bucket.getFileCount(), FormatUtils.formatFileSize(bucket.getTotalSize()));

        return convertToResponse(bucket);
    }

    @Transactional(readOnly = true)
    public BucketHealthResponse checkBucketHealth(String bucketName) {
        String normalized = normalizeBucketName(bucketName);
        LocalDateTime checkedAt = LocalDateTime.now();

        boolean existsInDb = bucketRepository
                .findByNameAndDeletedAtIsNull(normalized).isPresent();

        MinioReachabilityStatus reachability =
                minioService.checkBucketReachability(normalized);

        boolean storageReachable = reachability != MinioReachabilityStatus.NETWORK_ERROR;
        boolean existsInStorage = reachability == MinioReachabilityStatus.REACHABLE;

        boolean healthy = existsInDb && existsInStorage;
        String errorCode = null;
        String message;

        if (healthy) {
            message = "Bucket je potpuno ispravan i sinhronizovan.";

        } else if (reachability == MinioReachabilityStatus.NETWORK_ERROR) {
            errorCode = "STORAGE_UNREACHABLE";
            message = existsInDb
                    ? "Bucket postoji u bazi, ali storage server trenutno nije dostupan."
                    : "Bucket ne postoji u bazi, a storage server je nedostupan.";

        } else if (reachability == MinioReachabilityStatus.PERMISSION_DENIED) {
            errorCode = "PERMISSION_DENIED";
            message = "Storage server je dostupan, ali pristup bucketu je odbijen. " +
                    "Provjerite credentials.";

        } else if (!existsInDb && !existsInStorage) {
            errorCode = "NOT_FOUND_ANYWHERE";
            message = "Bucket ne postoji ni u bazi ni na storage serveru.";

        } else if (!existsInDb) {
            errorCode = "MISSING_IN_DB";
            message = "Bucket postoji na storage serveru ali nema zapisa u bazi. " +
                    "Pokrenite sync.";

        } else {
            errorCode = "MISSING_IN_STORAGE";
            message = "Bucket postoji u bazi ali je obrisan na storage serveru. " +
                    "Potrebna je intervencija.";
        }

        return BucketHealthResponse.builder()
                .bucketName(normalized)
                .existsInDatabase(existsInDb)
                .existsInStorage(existsInStorage)
                .storageReachable(storageReachable)
                .healthy(healthy)
                .errorCode(errorCode)
                .message(message)
                .checkedAt(checkedAt)
                .build();
    }

    private String normalizeBucketName(String name) {
        return name.trim().toLowerCase();
    }

    private void checkBucketLimit(User user) {
        long count = bucketRepository.countByOwnerId(user.getId());
        if (count >= maxBucketsPerUser) {
            throw AppException.badRequest("Dostignut je maksimalan broj bucketa po korisniku.");
        }
    }

    private BucketResponse convertToResponse(Bucket bucket) {
        BucketResponse response = new BucketResponse();
        response.setId(bucket.getId());
        response.setName(bucket.getName());
        response.setOwner(bucket.getOwnerUsername());
        response.setPublic(bucket.isPublic());
        response.setAllowPublicUpload(bucket.isAllowPublicUpload());
        response.setCreatedAt(bucket.getCreatedAt());
        response.setLastModifiedAt(bucket.getLastModifiedAt());
        response.setLastModifiedBy(bucket.getLastModifiedBy());
        response.setLastSyncAt(bucket.getLastSyncAt());
        response.setDescription(bucket.getDescription());
        response.setFileCount(bucket.getFileCount());
        response.setTotalSize(bucket.getTotalSize());
        response.setMaxSizeBytes(bucket.getMaxSizeBytes());
        response.setVersioningEnabled(bucket.isVersioningEnabled());
        response.setObjectLockEnabled(bucket.isObjectLockEnabled());
        return response;
    }

    private Bucket findActiveBucketById(Long id) {
        return bucketRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        AppException.notFound("Bucket sa ID: " + id + " nije pronađen"));
    }

    private Bucket findActiveBucketByName(String name) {
        return bucketRepository.findByNameAndDeletedAtIsNull(name)
                .orElseThrow(() ->
                        AppException.notFound("Bucket sa imenom: " + name + " nije pronađen"));
    }

    private List<BucketResponse> convertToResponseList(List<Bucket> buckets) {
        return buckets.stream()
                .map(this::convertToResponse)
                .toList();
    }

}