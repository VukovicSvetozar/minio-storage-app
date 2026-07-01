package com.minio.storage.service;

import com.minio.storage.dto.*;
import com.minio.storage.entity.Bucket;
import com.minio.storage.entity.FileMetadata;
import com.minio.storage.entity.FileVersion;
import com.minio.storage.entity.User;
import com.minio.storage.enums.FileCategory;
import com.minio.storage.enums.Role;
import com.minio.storage.exception.AppException;
import com.minio.storage.repository.BucketRepository;
import com.minio.storage.repository.FileMetadataRepository;
import com.minio.storage.repository.FileVersionRepository;
import com.minio.storage.util.ActivityLogHelper;
import com.minio.storage.util.FormatUtils;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final MinioService minioService;
    private final UserSecurityService userSecurityService;
    private final BucketSecurityService bucketSecurityService;
    private final FileSecurityService fileSecurityService;
    private final ShareLinkService shareLinkService;
    private final BucketRepository bucketRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileVersionRepository fileVersionRepository;
    private final ActivityLogHelper activityLogHelper;

    @Value("${app.storage.max-file-size:104857600}")
    private long maxFileSizeBytes;

    @Value("${app.stats.max-users}")
    private int maxUsersInStats;

    @Value("${app.stats.max-buckets}")
    private int maxBucketsInStats;

    @Transactional
    public FileUploadResponse uploadFile(
            String bucketName,
            MultipartFile file,
            String description,
            String tags,
            FileCategory category,
            boolean isPublic,
            HttpServletRequest request
    ) {
        validateUpload(file);
        fileSecurityService.checkCanUpload(bucketName);

        User currentUser = userSecurityService.getCurrentUser();

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            originalName = "unknown_file_" + System.currentTimeMillis();
        }
        String objectName = generateObjectName(originalName);

        int reserved = bucketRepository.tryReserveSpace(bucketName, file.getSize());
        if (reserved == 0) {
            throw AppException.badRequest("Bucket nema dovoljno prostora ili ne postoji.");
        }

        String minioVersionId;
        try {
            minioVersionId = minioService.putObject(
                    bucketName,
                    objectName,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
        } catch (IOException e) {
            log.error("Greška pri čitanju fajla: {}", e.getMessage());
            throw AppException.badRequest("Nije moguće pročitati sadržaj fajla.");
        }

        minioService.registerRollbackCleanup(bucketName, objectName, minioVersionId);

        FileMetadata metadata = FileMetadata.builder()
                .originalFileName(originalName)
                .objectName(objectName)
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .fileSize(file.getSize())
                .bucketName(bucketName)
                .owner(currentUser)
                .category(category != null ? category : FileCategory.OTHER)
                .description(description)
                .tags(tags)
                .isPublic(isPublic)
                .build();

        FileMetadata savedMetadata = fileMetadataRepository.save(metadata);

        if (minioVersionId != null) {
            FileVersion version = FileVersion.builder()
                    .fileMetadata(savedMetadata)
                    .minioVersionId(minioVersionId)
                    .storageKey(objectName)
                    .sizeBytes(file.getSize())
                    .versionNumber(1)
                    .isLatest(true)
                    .build();
            fileVersionRepository.save(version);
        }

        log.info("Fajl uspješno otpremljen: ID={}, bucket={}", savedMetadata.getId(), bucketName);

        try {
            activityLogHelper.logFileUpload(savedMetadata.getOriginalFileName(), savedMetadata.getId(), request);
        } catch (Exception e) {
            log.warn("Logovanje aktivnosti nije uspjelo (non-blocking): {}", e.getMessage());
        }

        return FileUploadResponse.builder()
                .fileId(savedMetadata.getId())
                .originalFileName(savedMetadata.getOriginalFileName())
                .objectName(savedMetadata.getObjectName())
                .bucketName(savedMetadata.getBucketName())
                .fileSize(savedMetadata.getFileSize())
                .formattedSize(savedMetadata.getFormattedSize())
                .contentType(savedMetadata.getContentType())
                .uploadDate(savedMetadata.getUploadedAt())
                .message("Fajl je uspješno otpremljen.")
                .build();
    }

    @Transactional
    public void deleteFile(Long fileId, HttpServletRequest request) {

        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> AppException.notFound("Traženi fajl nije pronađen."));

        fileSecurityService.checkCanDelete(file);

        Bucket bucket = bucketRepository
                .findByNameAndDeletedAtIsNull(file.getBucketName())
                .orElseThrow(() -> AppException.badRequest(
                        "Nije moguće obrisati fajl jer bucket ne postoji ili je obrisan."));

        String bucketName = bucket.getName();
        String objectName = file.getObjectName();
        String fileName = file.getOriginalFileName();

        List<FileVersion> versions = fileVersionRepository
                .findByFileMetadataIdOrderByVersionNumberDesc(fileId);

        long totalSizeToFree = versions.isEmpty()
                ? file.getFileSize()
                : versions.stream().mapToLong(FileVersion::getSizeBytes).sum();

        if (!versions.isEmpty()) {
            fileVersionRepository.deleteByFileId(fileId);
            log.info("Baza: obrisane verzije za fajl ID={}", fileId);
        }

        shareLinkService.deleteShareLinksByFileId(fileId);

        fileMetadataRepository.delete(file);

        int updated = bucketRepository.decrementStatsAfterDelete(bucketName, totalSizeToFree);
        if (updated == 0) {
            log.error("Statistika nije ažurirana: bucket={}, fileSizeToFree={}", bucketName, totalSizeToFree);
            throw AppException.internalError("Statistika bucketa nije ažurirana.");
        }

        if (versions.isEmpty()) {
            minioService.removeObject(bucketName, objectName);
            log.info("MinIO: obrisan objekat: {}/{}", bucketName, objectName);
        } else {
            List<String> failed = minioService.removeObjects(bucketName, versions);
            if (!failed.isEmpty()) {
                log.error("MINIO_PARTIAL_DELETE fileId={} failedCount={} failedItems={}",
                        fileId, failed.size(), failed);
            } else {
                log.info("MinIO: obrisano {} verzija za {}/{}",
                        versions.size(), bucketName, objectName);
            }
        }

        log.info("Brisanje završeno: ID={}, bucket={}", fileId, bucketName);

        try {
            activityLogHelper.logFileDelete(fileName, fileId, request);
        } catch (Exception e) {
            log.warn("Logovanje aktivnosti nije uspjelo za brisanje fajla ID={}: {}", fileId, e.getMessage());
        }

    }

    @Transactional
    public FileMetadataResponse updateFileMetadata(
            Long fileId,
            UpdateFileMetadataRequest request,
            HttpServletRequest httpRequest
    ) {
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() ->
                        AppException.notFound("Traženi fajl nije pronađen.")
                );

        fileSecurityService.checkCanModify(file);

        if (request.getDescription() != null) {
            file.setDescription(request.getDescription());
        }
        if (request.getTags() != null) {
            file.setTags(String.join(",", request.getTags()));
        }
        if (request.getCategory() != null) {
            file.setCategory(request.getCategory());
        }
        if (request.getIsPublic() != null) {
            file.setPublic(request.getIsPublic());
        }

        FileMetadata updated = fileMetadataRepository.save(file);
        log.info("Metapodaci fajla uspešno ažurirani: ID={}, Naziv={}", fileId, file.getOriginalFileName());

        try {
            activityLogHelper.logFileUpdate(file.getOriginalFileName(), fileId, httpRequest);
        } catch (Exception e) {
            log.warn("Neuspješno logovanje aktivnosti za izmjenu fajla ID {}: {}", fileId, e.getMessage());
        }

        return mapToResponse(updated);
    }

    @Transactional
    public FileMetadataResponse updateFileVisibility(
            Long fileId,
            UpdateFileVisibilityRequest request,
            HttpServletRequest httpRequest
    ) {
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> AppException.notFound("Traženi fajl nije pronađen."));

        fileSecurityService.checkCanModify(file);

        if (file.isPublic() == request.getIsPublic()) {
            return mapToResponse(file);
        }

        boolean oldValue = file.isPublic();

        file.setPublic(request.getIsPublic());
        FileMetadata updated = fileMetadataRepository.save(file);

        log.info("Promjena vidljivosti fajla: ID={}, {} -> {}",
                fileId, oldValue, request.getIsPublic());

        try {
            activityLogHelper.logFileUpdate(file.getOriginalFileName(), fileId, httpRequest);
        } catch (Exception e) {
            log.warn("Logovanje aktivnosti nije uspjelo za promjenu vidljivosti ID={}: {}",
                    fileId, e.getMessage());
        }

        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public FileMetadataResponse getFileById(Long fileId) {
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.warn("Pokušaj pristupa nepostojećem fajlu: ID={}", fileId);
                    return AppException.notFound("Fajl nije pronađen.");
                });

        fileSecurityService.checkCanView(file);

        return mapToResponse(file);
    }

    @Transactional(readOnly = true)
    public Page<FileMetadataResponse> getMyFiles(Pageable pageable) {
        User currentUser = userSecurityService.getCurrentUser();
        Page<FileMetadata> files = fileMetadataRepository.findByOwnerId(currentUser.getId(), pageable);
        return files.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<FileMetadataResponse> getPublicFiles(Pageable pageable) {
        Page<FileMetadata> files = fileMetadataRepository.findPublicFiles(pageable);
        return files.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<FileMetadataResponse> getFilesByCategory(
            FileCategory category,
            Pageable pageable
    ) {
        User currentUser = userSecurityService.getCurrentUserOrNull();
        Page<FileMetadata> files;

        String userEmail = (currentUser != null) ? currentUser.getEmail() : "anonymous";

        if (currentUser != null && currentUser.getRole() == Role.ADMIN) {
            log.info("Admin pretraga fajlova po kategoriji: {} | Korisnik: {}", category, userEmail);
            files = fileMetadataRepository.findByCategoryForAdmin(category, pageable);
        } else if (currentUser != null) {
            log.info("Korisnička pretraga fajlova po kategoriji: {} | Korisnik: {}", category, userEmail);
            files = fileMetadataRepository.findByCategoryAndAccessibleByUser(category, currentUser.getId(), pageable);
        } else {
            log.info("Javna pretraga fajlova po kategoriji: {}", category);
            files = fileMetadataRepository.findByCategoryAndPublic(category, pageable);
        }

        return files.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<FileMetadataResponse> getFilesByBucket(String bucketName, Pageable pageable) {

        bucketSecurityService.checkCanViewBucket(bucketName);

        Page<FileMetadata> files =
                fileMetadataRepository.findByBucketName(bucketName, pageable);

        return files.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<FileMetadataResponse> searchFiles(
            FileSearchRequest searchRequest,
            Pageable pageable
    ) {
        if (searchRequest.getMinSize() != null && searchRequest.getMaxSize() != null
                && searchRequest.getMinSize() > searchRequest.getMaxSize()) {
            throw AppException.badRequest("Minimalna veličina ne može biti veća od maksimalne.");
        }

        if (searchRequest.getUploadedAfter() != null && searchRequest.getUploadedBefore() != null
                && searchRequest.getUploadedAfter().isAfter(searchRequest.getUploadedBefore())) {
            throw AppException.badRequest("Datum 'od' ne može biti nakon datuma 'do'.");
        }

        if (searchRequest.getBucketName() != null && !searchRequest.getBucketName().isEmpty()) {
            bucketSecurityService.checkCanViewBucket(searchRequest.getBucketName());
        }

        User currentUser = userSecurityService.getCurrentUserOrNull();

        String username = (currentUser != null) ? currentUser.getUsername() : "anonymous";

        Specification<FileMetadata> spec = buildSearchSpecification(searchRequest);
        spec = spec.and(buildVisibilitySpecification(currentUser));

        log.info("Pokrenuta pretraga fajlova | Korisnik: {} | Pojam: '{}' | Kategorija: {}",
                username, searchRequest.getSearchTerm(), searchRequest.getCategory());

        Page<FileMetadata> files = fileMetadataRepository.findAll(spec, pageable);

        return files.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public PresignedUrlResponse generateDownloadLink(Long fileId, Integer expiresInSeconds, HttpServletRequest request) {
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.warn("Pokušaj generisanja linka za nepostojeći fajl: ID={}", fileId);
                    return AppException.notFound("Fajl nije pronađen.");
                });

        fileSecurityService.checkCanView(file);

        int expiry = (expiresInSeconds == null || expiresInSeconds <= 0) ? 900 : Math.min(expiresInSeconds, 7200);

        String contentDisposition = buildContentDisposition("attachment", file.getOriginalFileName());

        String url = minioService.getPresignedUrl(
                file.getBucketName(),
                file.getObjectName(),
                expiry,
                contentDisposition
        );

        try {
            activityLogHelper.logFileDownload(file.getOriginalFileName(), fileId, request);
        } catch (Exception e) {
            log.warn("Logovanje aktivnosti nije uspjelo za download-link ID={}: {}", fileId, e.getMessage());
        }

        return PresignedUrlResponse.builder()
                .url(url)
                .expiresIn(expiry)
                .expiresAt(Instant.now().plusSeconds(expiry))
                .build();
    }

    @Transactional(readOnly = true)
    public PresignedUrlResponse generatePreviewLink(Long fileId, Integer expiresInSeconds, HttpServletRequest request) {
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.warn("Pokušaj generisanja preview linka za nepostojeći fajl: ID={}", fileId);
                    return AppException.notFound("Fajl nije pronađen.");
                });

        fileSecurityService.checkCanView(file);

        int expiry = (expiresInSeconds == null || expiresInSeconds <= 0) ? 900 : Math.min(expiresInSeconds, 7200);
        String contentDisposition = buildContentDisposition("inline", file.getOriginalFileName());

        String url = minioService.getPresignedUrl(
                file.getBucketName(),
                file.getObjectName(),
                expiry,
                contentDisposition
        );

        try {
            activityLogHelper.logFilePreview(file.getOriginalFileName(), fileId, request);
        } catch (Exception e) {
            log.warn("Logovanje aktivnosti nije uspjelo za preview-link ID={}: {}", fileId, e.getMessage());
        }

        return PresignedUrlResponse.builder()
                .url(url)
                .expiresIn(expiry)
                .expiresAt(Instant.now().plusSeconds(expiry))
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> streamFile(Long fileId, HttpServletRequest request) {
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> AppException.notFound("Fajl nije pronađen."));

        fileSecurityService.checkCanView(file);

        InputStream stream;
        try {
            stream = minioService.getObject(file.getBucketName(), file.getObjectName());
        } catch (Exception e) {
            throw AppException.internalError("Nije moguće preuzeti fajl sa servera.");
        }

        MediaType mediaType;
        try {
            mediaType = (file.getContentType() != null && !file.getContentType().isBlank())
                    ? MediaType.parseMediaType(file.getContentType())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (InvalidMediaTypeException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            activityLogHelper.logFileDownload(file.getOriginalFileName(), fileId, request);
        } catch (Exception e) {
            log.warn("Logovanje aktivnosti nije uspjelo za proxy download ID={}: {}", fileId, e.getMessage());
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition("attachment", file.getOriginalFileName()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(mediaType)
                .contentLength(file.getFileSize())
                .body(new InputStreamResource(stream));
    }

    @Transactional(readOnly = true)
    public UserFileStatisticsResponse getMyStatistics() {
        User currentUser = userSecurityService.getCurrentUser();

        log.debug("Generisanje lične statistike fajlova za korisnika: {}", currentUser.getUsername());

        Object[] stats = fileMetadataRepository.getCountAndSizeByUser(currentUser.getId());

        long totalFiles = (stats != null && stats[0] != null) ? ((Number) stats[0]).longValue() : 0L;
        long totalSize = (stats != null && stats[1] != null) ? ((Number) stats[1]).longValue() : 0L;

        List<Object[]> categoryStats = fileMetadataRepository.getCategoryStatsByUser(currentUser.getId());
        Map<String, Long> byCategory = categoryStats.stream()
                .collect(Collectors.toMap(
                        row -> ((FileCategory) row[0]).name(),
                        row -> ((Number) row[1]).longValue(),
                        Long::sum
                ));

        List<Object[]> bucketStats = fileMetadataRepository.getBucketStatsByUser(currentUser.getId());
        Map<String, Long> byBucket = bucketStats.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue(),
                        Long::sum
                ));

        log.debug("Lična statistika uspješno generisana za korisnika {}. Fajlova: {}, Veličina: {}",
                currentUser.getUsername(), totalFiles, totalSize);

        return UserFileStatisticsResponse.builder()
                .username(currentUser.getUsername())
                .totalFiles(totalFiles)
                .totalSizeBytes(totalSize)
                .totalSizeFormatted(FormatUtils.formatFileSize(totalSize))
                .byCategory(byCategory)
                .byBucket(byBucket)
                .build();
    }

    @Transactional(readOnly = true)
    public FileStatisticsResponse getGlobalStatistics() {

        log.debug("Generisanje globalne statistike fajlova...");

        long totalFiles = fileMetadataRepository.countAllActiveFiles();
        long totalSize = fileMetadataRepository.sumAllActiveFileSizes();

        List<Object[]> categoryData = fileMetadataRepository.countByCategory();
        Map<String, Long> byCategory = categoryData.stream()
                .collect(Collectors.toMap(
                        row -> ((FileCategory) row[0]).name(),
                        row -> ((Number) row[1]).longValue(),
                        Long::sum
                ));

        List<Object[]> userData = fileMetadataRepository.getTotalStorageByUsers();
        Map<String, Long> byUser = userData.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue(),
                        Long::sum
                ));

        List<Object[]> bucketData = fileMetadataRepository.countFilesByBucket();
        Map<String, Long> byBucket = bucketData.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue(),
                        Long::sum
                ));

        log.debug("Globalna statistika završena. Fajlova: {}, Veličina: {}", totalFiles, totalSize);

        return FileStatisticsResponse.builder()
                .totalFiles(totalFiles)
                .totalSizeBytes(totalSize)
                .totalSizeFormatted(FormatUtils.formatFileSize(totalSize))
                .byCategory(byCategory)
                .byUser(byUser)
                .byBucket(byBucket)
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserStorageStatisticsResponse> getUserStorageStatistics() {

        log.debug("Generisanje izvještaja o zauzeću skladišta po korisnicima...");

        List<Object[]> results =
                fileMetadataRepository.getUserStorageStatsComplete(PageRequest.of(0, maxUsersInStats));

        return results.stream()
                .map(row -> {
                    String username = (String) row[0];
                    long fileCount = ((Number) row[1]).longValue();
                    long totalSize = ((Number) row[2]).longValue();

                    return UserStorageStatisticsResponse.builder()
                            .username(username)
                            .fileCount(fileCount)
                            .totalSizeBytes(totalSize)
                            .totalSizeFormatted(FormatUtils.formatFileSize(totalSize))
                            .build();
                })
                .collect(Collectors.toList());

    }

    @Transactional(readOnly = true)
    public List<CategoryStatisticsResponse> getCategoryStatistics() {

        log.debug("Generisanje statistike po kategorijama fajlova...");

        List<Object[]> results = fileMetadataRepository.getCategoryStatsComplete();

        return results.stream()
                .map(row -> {
                    String category = (String) row[0];
                    long count = ((Number) row[1]).longValue();
                    long totalSize = ((Number) row[2]).longValue();
                    long avgSize = count > 0 ? totalSize / count : 0L;

                    return CategoryStatisticsResponse.builder()
                            .category(category)
                            .fileCount(count)
                            .totalSizeBytes(totalSize)
                            .totalSizeFormatted(FormatUtils.formatFileSize(totalSize))
                            .averageSizeBytes(avgSize)
                            .averageSizeFormatted(FormatUtils.formatFileSize(avgSize))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BucketStatisticsResponse> getBucketStatistics() {

        log.debug("Generisanje statistike zauzeća po bucketima...");

        List<Object[]> results = fileMetadataRepository.getBucketStorageStats(PageRequest.of(0, maxBucketsInStats));

        return results.stream()
                .map(row -> {
                    String bucketName = (String) row[0];
                    long count = ((Number) row[1]).longValue();
                    long totalSize = ((Number) row[2]).longValue();

                    return BucketStatisticsResponse.builder()
                            .bucketName(bucketName)
                            .fileCount(count)
                            .totalSizeBytes(totalSize)
                            .totalSizeFormatted(FormatUtils.formatFileSize(totalSize))
                            .build();
                })
                .toList();
    }

    private void validateUpload(MultipartFile file) {
        if (file.isEmpty()) {
            throw AppException.badRequest("Izabrani fajl je prazan.");
        }

        if (file.getSize() > maxFileSizeBytes) {
            log.warn("Pokušaj otpremanja prevelikog fajla: {} (veličina: {} bajtova)",
                    file.getOriginalFilename(), file.getSize());
            throw AppException.badRequest("Fajl je veći od maksimalno dozvoljene veličine.");
        }

        String contentType = file.getContentType();
        if (contentType == null || "application/octet-stream".equals(contentType)) {
            log.warn("Otpremanje fajla sa generičkim tipom (content-type): naziv='{}', veličina={} bajtova",
                    file.getOriginalFilename(),
                    file.getSize()
            );
        }
    }

    private String generateObjectName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }

    private FileMetadataResponse mapToResponse(FileMetadata file) {
        return FileMetadataResponse.builder()
                .id(file.getId())
                .originalFileName(file.getOriginalFileName())
                .objectName(file.getObjectName())
                .contentType(file.getContentType())
                .fileSize(file.getFileSize())
                .formattedSize(file.getFormattedSize())
                .bucketName(file.getBucketName())
                .uploadDate(file.getUploadedAt())
                .uploadedBy(file.getOwnerUsername())
                .description(file.getDescription())
                .tags(file.getTagsArray())
                .category(file.getCategory().name())
                .isPublic(file.isPublic())
                .lastModified(file.getUpdatedAt())
                .extension(file.getExtension())
                .isImage(file.isImage())
                .thumbnailPath(null)
                .downloadUrl(null)
                .build();
    }

    private Specification<FileMetadata> buildSearchSpecification(FileSearchRequest request) {
        Specification<FileMetadata> spec = Specification.where(null);

        if (request.getSearchTerm() != null && !request.getSearchTerm().isBlank()) {
            String searchTerm = request.getSearchTerm().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("originalFileName")), "%" + searchTerm + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + searchTerm + "%"),
                    cb.like(cb.lower(root.get("tags")), "%" + searchTerm + "%")
            ));
        }

        if (request.getBucketName() != null && !request.getBucketName().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("bucketName"), request.getBucketName())
            );
        }

        if (request.getCategory() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category"), request.getCategory())
            );
        }

        if (request.getContentType() != null && !request.getContentType().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("contentType")), request.getContentType().toLowerCase() + "%")
            );
        }

        if (request.getIsPublic() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isPublic"), request.getIsPublic())
            );
        }

        if (request.getUploadedBy() != null && !request.getUploadedBy().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("owner").get("username"), request.getUploadedBy())
            );
        }

        if (request.getUploadedAfter() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("uploadedAt"), request.getUploadedAfter())
            );
        }

        if (request.getUploadedBefore() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("uploadedAt"), request.getUploadedBefore())
            );
        }

        if (request.getMinSize() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("fileSize"), request.getMinSize())
            );
        }

        if (request.getMaxSize() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("fileSize"), request.getMaxSize())
            );
        }

        return spec;
    }

    private Specification<FileMetadata> buildVisibilitySpecification(User currentUser) {
        return (root, query, cb) -> {

            if (currentUser != null && currentUser.getRole() == Role.ADMIN) {
                return cb.conjunction();
            }

            boolean isCountQuery = query != null && Long.class.equals(query.getResultType());

            Predicate isFilePublic = cb.isTrue(root.get("isPublic"));

            if (isCountQuery || query == null) {
                if (currentUser != null) {
                    Predicate isOwner = cb.equal(root.get("owner").get("id"), currentUser.getId());
                    return cb.or(isFilePublic, isOwner);
                }
                return isFilePublic;
            }

            Subquery<String> publicBucketSubquery = query.subquery(String.class);
            Root<Bucket> bRoot = publicBucketSubquery.from(Bucket.class);
            publicBucketSubquery.select(bRoot.get("name"))
                    .where(cb.and(
                            cb.isTrue(bRoot.get("isPublic")),
                            cb.isNull(bRoot.get("deletedAt"))
                    ));

            Predicate isInPublicBucket = root.get("bucketName").in(publicBucketSubquery);

            if (currentUser != null) {
                Predicate isOwner = cb.equal(root.get("owner").get("id"), currentUser.getId());

                Subquery<String> ownedBucketSubquery = query.subquery(String.class);
                Root<Bucket> bOwned = ownedBucketSubquery.from(Bucket.class);
                ownedBucketSubquery.select(bOwned.get("name"))
                        .where(cb.and(
                                cb.equal(bOwned.get("owner").get("id"), currentUser.getId()),
                                cb.isNull(bOwned.get("deletedAt"))
                        ));

                Predicate isInOwnedBucket = root.get("bucketName").in(ownedBucketSubquery);

                return cb.or(isFilePublic, isInPublicBucket, isOwner, isInOwnedBucket);
            }

            return cb.or(isFilePublic, isInPublicBucket);
        };
    }

    private String buildContentDisposition(String disposition, String originalFileName) {
        String safeFilename = originalFileName.replaceAll("[\"\\r\\n]", "_");
        String encoded = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return disposition + "; filename=\"" + safeFilename + "\"; filename*=UTF-8''" + encoded;
    }

}