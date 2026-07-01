package com.minio.storage.service;

import com.minio.storage.dto.BucketSyncResult;
import com.minio.storage.enums.BucketSyncStatus;
import com.minio.storage.entity.Bucket;
import com.minio.storage.entity.User;
import com.minio.storage.repository.BucketRepository;
import com.minio.storage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BucketSyncWorker {

    private final BucketRepository bucketRepository;
    private final MinioService minioService;
    private final FileMetadataRepository fileMetadataRepository;

    @Value("${app.bucket.default-quota-bytes:10737418240}")
    private Long defaultQuotaBytes;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BucketSyncResult syncSingleBucket(String name, User systemUser) {

        Optional<Bucket> existingOpt = bucketRepository.findByName(name);

        if (existingOpt.isEmpty()) {
            Bucket bucket = new Bucket(
                    name,
                    systemUser,
                    minioService.isBucketPublic(name),
                    defaultQuotaBytes
            );
            bucket.setDescription("Automatski kreirano tokom sinhronizacije sa storage serverom.");
            updateStatsFromDb(bucket);
            bucketRepository.save(bucket);

            log.info("SYNC: Uvezen novi bucket: '{}'", name);
            return new BucketSyncResult(name, BucketSyncStatus.IMPORTED,
                    bucket.getFileCount(), bucket.getTotalSize(), bucket.isPublic());

        } else if (existingOpt.get().isActive()) {
            Bucket bucket = existingOpt.get();
            bucket.setPublic(minioService.isBucketPublic(name));
            updateStatsFromDb(bucket);
            bucketRepository.save(bucket);

            log.info("SYNC: Ažuriran bucket: '{}'", name);
            return new BucketSyncResult(name, BucketSyncStatus.UPDATED,
                    bucket.getFileCount(), bucket.getTotalSize(), bucket.isPublic());

        } else {
            log.warn("SYNC: Bucket '{}' postoji na storage serveru ali je soft-deleted u bazi.", name);
            return new BucketSyncResult(name, BucketSyncStatus.SKIPPED, null, null, false);
        }
    }

    private void updateStatsFromDb(Bucket bucket) {
        try {
            Object[] stats = fileMetadataRepository.getBucketStatistics(bucket.getName());
            if (stats != null && stats.length >= 2) {
                long fileCount = stats[0] != null ? ((Number) stats[0]).longValue() : 0L;
                long totalSize = stats[1] != null ? ((Number) stats[1]).longValue() : 0L;
                bucket.syncStatistics(fileCount, totalSize);
            } else {
                bucket.syncStatistics(0L, 0L);
            }
        } catch (Exception e) {
            log.warn("SYNC: Neuspješno čitanje statistike za bucket '{}': {}",
                    bucket.getName(), e.getMessage());
            bucket.syncStatistics(0L, 0L);
        }
    }

}