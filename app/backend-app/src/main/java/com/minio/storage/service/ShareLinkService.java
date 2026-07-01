package com.minio.storage.service;

import com.minio.storage.dto.CreateShareLinkRequest;
import com.minio.storage.dto.ShareLinkAccessResponse;
import com.minio.storage.dto.ShareLinkResponse;
import com.minio.storage.entity.FileMetadata;
import com.minio.storage.entity.ShareLink;
import com.minio.storage.entity.User;
import com.minio.storage.enums.Role;
import com.minio.storage.exception.AppException;
import com.minio.storage.repository.FileMetadataRepository;
import com.minio.storage.repository.ShareLinkRepository;
import com.minio.storage.util.FormatUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserSecurityService userSecurityService;
    private final FileSecurityService fileSecurityService;
    private final MinioService minioService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.share-link.presigned-url-ttl-seconds}")
    private int presignedUrlTtlSeconds;

    @Transactional
    public ShareLinkResponse createShareLink(Long fileId, CreateShareLinkRequest request) {
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> AppException.notFound(
                        "Fajl sa ID-em " + fileId + " nije pronađen."));

        fileSecurityService.checkCanShare(file);

        String currentUsername = userSecurityService.getCurrentUser().getUsername();

        ShareLink shareLink = ShareLink.builder()
                .shareToken(generateUniqueToken())
                .file(file)
                .createdBy(currentUsername)
                .expiresAt(LocalDateTime.now().plusHours(request.getExpirationHours()))
                .description(request.getDescription())
                .build();

        ShareLink saved = shareLinkRepository.save(shareLink);

        log.info("Share link kreiran: token={}, fajl='{}', korisnik='{}'",
                saved.getShareToken(), file.getOriginalFileName(), saved.getCreatedBy());

        return convertToResponse(saved);
    }

    @Transactional
    public ShareLinkAccessResponse accessShareLink(String shareToken) {
        ShareLink shareLink = shareLinkRepository.findByShareToken(shareToken)
                .orElseThrow(() -> AppException.notFound("Share link nije pronađen."));

        if (!shareLink.isValid()) {
            String reason = shareLink.isExpired() ? "istekao" : "deaktiviran";
            throw AppException.gone("Share link je " + reason + ".");
        }

        shareLink.incrementAccessCount();
        shareLinkRepository.save(shareLink);

        FileMetadata file = shareLink.getFile();

        String safeFilename = file.getOriginalFileName().replaceAll("[\"\\r\\n]", "_");
        String contentDisposition = "attachment; filename=\"" + safeFilename + "\"";

        String downloadUrl = minioService.getPresignedUrl(
                file.getBucketName(),
                file.getObjectName(),
                presignedUrlTtlSeconds,
                contentDisposition
        );

        log.info("Pristup share linku [token={}]: fajl='{}' (ID={}), broj pristupa={}",
                shareToken, file.getOriginalFileName(), file.getId(), shareLink.getAccessCount());

        return ShareLinkAccessResponse.builder()
                .originalFileName(file.getOriginalFileName())
                .contentType(file.getContentType())
                .fileSize(file.getFileSize())
                .formattedSize(FormatUtils.formatFileSize(file.getFileSize()))
                .downloadUrl(downloadUrl)
                .description(shareLink.getDescription())
                .sharedBy(shareLink.getCreatedBy())
                .accessCount(shareLink.getAccessCount())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ShareLinkResponse> getMyShareLinks() {
        String username = userSecurityService.getCurrentUser().getUsername();
        return shareLinkRepository.findByCreatedByOrderByCreatedAtDesc(username)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShareLinkResponse> getMyActiveShareLinks() {
        String username = userSecurityService.getCurrentUser().getUsername();
        return shareLinkRepository.findValidByCreatedBy(username, LocalDateTime.now())
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShareLinkResponse getShareLinkStats(Long shareId) {
        User currentUser = userSecurityService.getCurrentUser();
        ShareLink shareLink = findShareLinkWithAccessCheck(shareId, currentUser);
        return convertToResponse(shareLink);
    }

    @Transactional
    public void deactivateShareLink(Long shareId) {
        User currentUser = userSecurityService.getCurrentUser();
        ShareLink shareLink = findShareLinkWithAccessCheck(shareId, currentUser);
        if (!shareLink.isActive()) {
            log.info("Share link {} je već deaktiviran.", shareId);
            return;
        }
        shareLink.deactivate();
        shareLinkRepository.save(shareLink);
        log.info("Share link deaktiviran: id={}, izvršio='{}'", shareId, currentUser.getUsername());
    }

    @Transactional
    public void deleteShareLink(Long shareId) {
        User currentUser = userSecurityService.getCurrentUser();
        ShareLink shareLink = findShareLinkWithAccessCheck(shareId, currentUser);
        shareLinkRepository.delete(shareLink);
        log.info("Share link obrisan: id={}, izvršio='{}'", shareId, currentUser.getUsername());
    }

    @Transactional
    public void deleteShareLinksByFileId(Long fileId) {
        shareLinkRepository.deleteByFileId(fileId);
        log.info("Obrisani share linkovi za fajl ID={}", fileId);
    }

    @Transactional
    public void deactivateShareLinksByUsername(String username) {
        shareLinkRepository.deactivateAllByCreatedBy(username);
        log.info("Svi share linkovi su deaktivirani za korisnika: {}", username);
    }

    private String generateUniqueToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private ShareLinkResponse convertToResponse(ShareLink shareLink) {
        return ShareLinkResponse.builder()
                .id(shareLink.getId())
                .shareToken(shareLink.getShareToken())
                .shareUrl(baseUrl + "/api/share/public/" + shareLink.getShareToken())
                .fileId(shareLink.getFile().getId())
                .originalFileName(shareLink.getFile().getOriginalFileName())
                .createdBy(shareLink.getCreatedBy())
                .createdAt(shareLink.getCreatedAt())
                .expiresAt(shareLink.getExpiresAt())
                .timeRemaining(shareLink.getTimeRemaining())
                .accessCount(shareLink.getAccessCount())
                .lastAccessedAt(shareLink.getLastAccessedAt())
                .isActive(shareLink.isActive())
                .isExpired(shareLink.isExpired())
                .description(shareLink.getDescription())
                .build();
    }

    private ShareLink findShareLinkWithAccessCheck(Long shareId, User currentUser) {
        ShareLink shareLink = shareLinkRepository.findById(shareId)
                .orElseThrow(() -> AppException.notFound(
                        "Share link sa ID-em " + shareId + " nije pronađen."));
        if (currentUser.getRole() == Role.ADMIN) return shareLink;
        if (!shareLink.getCreatedBy().equals(currentUser.getUsername())) {
            throw AppException.forbidden("Nemate dozvolu za pristup ovom share linku.");
        }
        return shareLink;
    }

}