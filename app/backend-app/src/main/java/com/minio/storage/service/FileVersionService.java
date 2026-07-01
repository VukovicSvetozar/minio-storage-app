package com.minio.storage.service;

import com.minio.storage.dto.*;
import com.minio.storage.entity.Bucket;
import com.minio.storage.entity.FileMetadata;
import com.minio.storage.entity.FileVersion;
import com.minio.storage.exception.AppException;
import com.minio.storage.repository.BucketRepository;
import com.minio.storage.repository.FileMetadataRepository;
import com.minio.storage.repository.FileVersionRepository;
import com.minio.storage.util.ActivityLogHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileVersionService {

    private final MinioService minioService;
    private final FileSecurityService fileSecurityService;
    private final BucketRepository bucketRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileVersionRepository fileVersionRepository;
    private final ActivityLogHelper activityLogHelper;

    @Transactional(readOnly = true)
    public Page<FileVersionSummaryResponse> listVersions(Long fileId, Pageable pageable) {

        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> AppException.notFound("Fajl sa ID " + fileId + " nije pronađen."));

        fileSecurityService.checkCanAccessVersions(file);

        log.debug("Dohvatanje verzija za fajl ID={}", fileId);

        Page<FileVersion> versions =
                fileVersionRepository.findByFileMetadataId(fileId, pageable);

        return versions.map(this::mapToSummary);
    }

    @Transactional(readOnly = true)
    public FileVersionDetailResponse getVersionDetails(Long fileId, Long versionId) {

        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> AppException.notFound("Fajl sa ID " + fileId + " nije pronađen."));

        fileSecurityService.checkCanAccessVersions(file);

        FileVersion version = fileVersionRepository.findByIdAndFileMetadataId(versionId, fileId)
                .orElseThrow(() -> AppException.notFound("Tražena verzija nije pronađena ili ne pripada ovom fajlu."));

        log.debug("Dohvaćeni detalji za verziju ID={} fajla ID={}", versionId, fileId);

        return mapToDetailsResponse(file, version);

    }

    @Transactional(readOnly = true)
    public PresignedUrlResponse generateDownloadLink(Long fileId, Long versionId, Integer expiresInSeconds, HttpServletRequest request) {
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> AppException.notFound("Fajl nije pronađen."));

        fileSecurityService.checkCanAccessVersions(file);

        FileVersion version = fileVersionRepository
                .findByIdAndFileMetadataId(versionId, fileId)
                .orElseThrow(() -> AppException.notFound("Verzija nije pronađena."));

        int expiry = (expiresInSeconds == null || expiresInSeconds <= 0) ? 900 : Math.min(expiresInSeconds, 7200);

        String safeFilename = file.getOriginalFileName().replaceAll("[\"\\r\\n]", "_");

        String contentDisposition = "attachment; filename=\"" + safeFilename + "\"";

        String url = minioService.getPresignedUrlForVersion(
                file.getBucketName(),
                version.getStorageKey(),
                version.getMinioVersionId(),
                expiry,
                contentDisposition
        );

        try {
            activityLogHelper.logFileDownload(file.getOriginalFileName() + " (verzija " + version.getVersionNumber() + ")", fileId, request);
        } catch (Exception e) {
            log.warn("Logovanje aktivnosti nije uspjelo za download verzije ID={}", versionId);
        }

        return PresignedUrlResponse.builder()
                .url(url)
                .expiresIn(expiry)
                .expiresAt(Instant.now().plusSeconds(expiry))
                .build();
    }

    @Transactional
    public MessageResponse restoreVersion(Long fileId, Long versionId, HttpServletRequest request) {

        FileMetadata file = fileMetadataRepository.findByIdWithLock(fileId)
                .orElseThrow(() -> AppException.notFound("Fajl nije pronađen."));

        fileSecurityService.checkCanModify(file);

        FileVersion oldVersion = fileVersionRepository.findByIdAndFileMetadataId(versionId, fileId)
                .orElseThrow(() -> AppException.notFound("Tražena verzija nije pronađena."));

        if (oldVersion.isLatest()) {
            throw AppException.badRequest("Ova verzija je već aktivna.");
        }

        String bucketName = file.getBucketName();

        int reserved = bucketRepository.tryReserveSizeOnly(bucketName, oldVersion.getSizeBytes());
        if (reserved == 0) {
            throw AppException.badRequest("Nema dovoljno prostora u bucketu za vraćanje ove verzije.");
        }

        String objectName = file.getObjectName();
        String newMinioVersionId = minioService.copyObject(bucketName, objectName, oldVersion.getMinioVersionId());

        if (newMinioVersionId == null) {
            throw AppException.badRequest("Vraćanje nije moguće (verzioniranje nije uključeno na MinIO).");
        }

        minioService.registerRollbackCleanup(bucketName, objectName, newMinioVersionId);

        // 3. Ažuriranje baze
        fileVersionRepository.clearLatestFlag(fileId);
        int nextVersionNumber = getNextVersionNumber(fileId);

        FileVersion newVersion = FileVersion.builder()
                .fileMetadata(file)
                .minioVersionId(newMinioVersionId)
                .storageKey(objectName)
                .sizeBytes(oldVersion.getSizeBytes())
                .checksum(oldVersion.getChecksum())
                .versionNumber(nextVersionNumber)
                .isLatest(true)
                .build();

        fileVersionRepository.save(newVersion);

        file.setFileSize(newVersion.getSizeBytes());
        fileMetadataRepository.save(file);

        try {
            activityLogHelper.logFileUpdate(
                    file.getOriginalFileName() + " (Vraćena verzija v" + oldVersion.getVersionNumber() + ")",
                    fileId, request);
        } catch (Exception e) {
            log.warn("Logovanje aktivnosti nije uspjelo za vraćanje verzije ID={}", fileId);
        }

        return new MessageResponse("Verzija je uspješno vraćena i sada je najnovija (v" + nextVersionNumber + ").");
    }

    @Transactional
    public MessageResponse deleteVersion(Long fileId, Long versionId, HttpServletRequest request) {

        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> AppException.notFound("Fajl nije pronađen."));

        FileVersion version = fileVersionRepository
                .findByIdAndFileMetadataId(versionId, fileId)
                .orElseThrow(() -> AppException.notFound("Verzija nije pronađena."));

        long versionCount = fileVersionRepository.countByFileMetadataId(fileId);
        if (versionCount <= 1) {
            throw AppException.badRequest("Nije moguće obrisati jedinu verziju fajla.");
        }

        boolean wasLatest = version.isLatest();
        String bucketName = file.getBucketName();
        String objectName = version.getStorageKey();

        fileVersionRepository.delete(version);
        fileVersionRepository.flush();

        if (wasLatest) {
            FileVersion newLatest = fileVersionRepository
                    .findTopByFileMetadataIdOrderByVersionNumberDesc(fileId)
                    .orElseThrow(() -> AppException.internalError(
                            "Greška pri određivanju nove najnovije verzije."));
            newLatest.setLatest(true);
            fileVersionRepository.save(newLatest);
            file.setFileSize(newLatest.getSizeBytes());
            fileMetadataRepository.save(file);
            log.info("Verzija v{} promovisana u 'latest' za fajl ID={}",
                    newLatest.getVersionNumber(), fileId);
        }

        int updated = bucketRepository.decrementSizeOnly(bucketName, version.getSizeBytes());
        if (updated == 0) {
            log.error("Statistika nije ažurirana: bucket={}, size={}", bucketName, version.getSizeBytes());
            throw AppException.internalError("Statistika bucketa nije ažurirana.");
        }

        minioService.removeObject(bucketName, objectName, version.getMinioVersionId());

        try {
            activityLogHelper.logFileDelete(
                    file.getOriginalFileName() + " (verzija " + version.getVersionNumber() + ")",
                    fileId, request);
        } catch (Exception e) {
            log.warn("Logovanje aktivnosti nije uspjelo: fileId={}, versionId={}", fileId, versionId);
        }

        return new MessageResponse("Verzija je uspješno obrisana.");
    }

    @Transactional
    public FileUploadResponse uploadNewVersion(Long fileId, MultipartFile file, HttpServletRequest request) {

        FileMetadata existingFile = fileMetadataRepository.findByIdWithLock(fileId)
                .orElseThrow(() -> AppException.notFound("Fajl sa ID " + fileId + " nije pronađen."));

        fileSecurityService.checkCanModify(existingFile);

        Bucket bucket = bucketRepository.findByNameAndDeletedAtIsNull(existingFile.getBucketName())
                .orElseThrow(() -> AppException.notFound("Bucket nije pronađen ili je obrisan."));

        if (!bucket.isVersioningEnabled()) {
            throw AppException.badRequest("Verzioniranje nije uključeno za bucket '" + bucket.getName() + "'.");
        }

        if (file.isEmpty()) {
            throw AppException.badRequest("Fajl je prazan.");
        }

        int reserved = bucketRepository.tryReserveSizeOnly(bucket.getName(), file.getSize());
        if (reserved == 0) {
            throw AppException.badRequest("Nema dovoljno prostora u bucketu za novu verziju.");
        }

        String newMinioVersionId;
        try {
            newMinioVersionId = minioService.putObject(
                    bucket.getName(),
                    existingFile.getObjectName(),
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
        } catch (IOException e) {
            log.error("Greška pri čitanju fajla: {}", e.getMessage());
            throw AppException.badRequest("Nije moguće pročitati sadržaj fajla.");
        }

        if (newMinioVersionId == null) {
            throw AppException.badRequest("Upload nije moguć (MinIO verzioniranje nije aktivno).");
        }

        minioService.registerRollbackCleanup(bucket.getName(), existingFile.getObjectName(), newMinioVersionId);

        fileVersionRepository.clearLatestFlag(fileId);
        int nextVersionNumber = getNextVersionNumber(fileId);

        FileVersion newVersion = FileVersion.builder()
                .fileMetadata(existingFile)
                .minioVersionId(newMinioVersionId)
                .storageKey(existingFile.getObjectName())
                .sizeBytes(file.getSize())
                .versionNumber(nextVersionNumber)
                .isLatest(true)
                .build();

        fileVersionRepository.save(newVersion);

        existingFile.setFileSize(file.getSize());
        if (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()) {
            existingFile.setOriginalFileName(file.getOriginalFilename());
        }
        if (file.getContentType() != null) {
            existingFile.setContentType(file.getContentType());
        }
        fileMetadataRepository.save(existingFile);

        try {
            activityLogHelper.logFileUpdate(
                    existingFile.getOriginalFileName() + " (nova verzija v" + nextVersionNumber + ")",
                    fileId, request);
        } catch (Exception e) {
            log.warn("Logovanje aktivnosti nije uspjelo za novu verziju fileId={}", fileId);
        }

        return FileUploadResponse.builder()
                .fileId(existingFile.getId())
                .originalFileName(existingFile.getOriginalFileName())
                .objectName(existingFile.getObjectName())
                .bucketName(bucket.getName())
                .fileSize(file.getSize())
                .formattedSize(existingFile.getFormattedSize())
                .contentType(existingFile.getContentType())
                .uploadDate(newVersion.getCreatedAt())
                .message("Verzija v" + nextVersionNumber + " je uspješno otpremljena.")
                .build();
    }

    private FileVersionSummaryResponse mapToSummary(FileVersion version) {
        return FileVersionSummaryResponse.builder()
                .id(version.getId())
                .minioVersionId(version.getMinioVersionId())
                .versionNumber(version.getVersionNumber())
                .size(version.getSizeBytes())
                .uploadedAt(version.getCreatedAt())
                .uploadedBy(version.getCreatedBy())
                .isLatest(version.isLatest())
                .build();
    }

    private FileVersionDetailResponse mapToDetailsResponse(
            FileMetadata file,
            FileVersion v) {
        return FileVersionDetailResponse.builder()
                .id(v.getId())
                .fileId(file.getId())
                .fileName(file.getOriginalFileName())
                .minioVersionId(v.getMinioVersionId())
                .versionNumber(v.getVersionNumber())
                .size(v.getSizeBytes())
                .checksum(v.getChecksum())
                .storageKey(v.getStorageKey())
                .uploadedAt(v.getCreatedAt())
                .uploadedBy(v.getCreatedBy())
                .isLatest(v.isLatest())
                .build();
    }

    private int getNextVersionNumber(Long fileId) {
        return fileVersionRepository.findMaxVersionNumber(fileId).orElse(0) + 1;
    }

}