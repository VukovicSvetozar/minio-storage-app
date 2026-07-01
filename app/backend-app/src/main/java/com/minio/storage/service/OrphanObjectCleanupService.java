package com.minio.storage.service;

import com.minio.storage.dto.OrphanCleanupReport;
import com.minio.storage.exception.AppException;
import com.minio.storage.repository.BucketRepository;
import com.minio.storage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrphanObjectCleanupService {

    private final BucketRepository bucketRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final MinioService minioService;

    @Transactional(readOnly = true)
    public OrphanCleanupReport findOrphans(String bucketName) {
        bucketRepository.findByNameAndDeletedAtIsNull(bucketName)
                .orElseThrow(() -> AppException.notFound("Bucket '" + bucketName + "' nije pronađen."));

        Set<String> minioObjects = new HashSet<>(minioService.listObjects(bucketName));
        Set<String> dbObjects = new HashSet<>(fileMetadataRepository.findAllObjectNamesByBucketName(bucketName));

        List<String> orphansInMinio = minioObjects.stream()
                .filter(obj -> !dbObjects.contains(obj))
                .toList();

        List<String> orphansInDatabase = dbObjects.stream()
                .filter(obj -> !minioObjects.contains(obj))
                .toList();

        int total = orphansInMinio.size() + orphansInDatabase.size();

        return OrphanCleanupReport.builder()
                .bucketName(bucketName)
                .orphansInMinio(orphansInMinio)
                .orphansInDatabase(orphansInDatabase)
                .totalOrphans(total)
                .build();
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional(readOnly = true)
    public void scheduledAuditLog() {
        log.info("Započinje se noćni audit integriteta fajlova...");
        bucketRepository.findAllByDeletedAtIsNull().forEach(bucket -> {
            try {
                OrphanCleanupReport report = findOrphans(bucket.getName());
                if (report.getTotalOrphans() > 0) {
                    log.warn("AUDIT ALERT: Bucket '{}' ima nekonzistentnosti! MinIO siročad: {}, DB siročad: {}",
                            report.getBucketName(), report.getOrphansInMinio().size(), report.getOrphansInDatabase().size());
                }
            } catch (Exception e) {
                log.error("AUDIT ERROR: Neuspješan pregled za bucket '{}': {}", bucket.getName(), e.getMessage());
            }
        });
        log.info("Noćni audit integriteta fajlova je završen.");
    }

}