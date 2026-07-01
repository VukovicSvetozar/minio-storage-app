package com.minio.storage.util;

import com.minio.storage.dto.BucketSyncResponse;
import com.minio.storage.entity.User;
import com.minio.storage.enums.ActivityType;
import com.minio.storage.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ActivityLogHelper {

    private final ActivityLogService activityLogService;

    public void logLoginFailure(String username, String error, HttpServletRequest request) {
        activityLogService.logFailure(ActivityType.LOGIN, username, "Neuspješna prijava na sistem", error, request);
    }

    public void logRegistration(String username, HttpServletRequest request) {
        activityLogService.logActivity(ActivityType.REGISTER, username, "Registracija novog korisnika", "USER", null, username, request);
    }

    public void logEmailVerification(String username, HttpServletRequest request) {
        activityLogService.logActivity(ActivityType.EMAIL_VERIFIED, username, "Email adresa uspješno verifikovana", "USER", null, username, request);
    }

    public void logResendVerification(String username, HttpServletRequest request) {
        activityLogService.logActivity(ActivityType.RESEND_VERIFICATION, username, "Ponovno slanje verifikacionog emaila", "USER", null, username, request);
    }

    public void logLoginSuccess(String username, HttpServletRequest request) {
        activityLogService.logActivity(ActivityType.LOGIN, username, "Uspješna prijava na sistem", "USER", null, username, request);
    }

    public void logPasswordResetRequest(String email, HttpServletRequest request) {
        activityLogService.logActivity(ActivityType.PASSWORD_RESET_REQUEST, email, "Zatražen reset lozinke", "USER", null, email, request);
    }

    public void logPasswordResetSuccess(String username, HttpServletRequest request) {
        activityLogService.logActivity(ActivityType.PASSWORD_RESET_SUCCESS, username, "Lozinka uspješno promijenjena", "USER", null, username, request);
    }

    @SuppressWarnings("unused")
    public void logLogout(String username, HttpServletRequest request) {
        activityLogService.logActivity(ActivityType.LOGOUT, username, "Odjava sa svih uređaja", "USER", null, username, request);
    }

    public void logFileUpload(String fileName, Long fileId, HttpServletRequest request) {
        String username = getCurrentUsername();
        activityLogService.logActivity(ActivityType.FILE_UPLOAD, username, "Postavljen fajl: " + fileName, "FILE", fileId, fileName, request);
    }

    public void logFileDownload(String fileName, Long fileId, HttpServletRequest request) {
        String username = getCurrentUsername();
        activityLogService.logActivity(ActivityType.FILE_DOWNLOAD, username, "Preuzet fajl: " + fileName, "FILE", fileId, fileName, request);
    }

    public void logFilePreview(String fileName, Long fileId, HttpServletRequest request) {
        String username = getCurrentUsername();
        activityLogService.logActivity(ActivityType.FILE_PREVIEW, username, "Pregled fajla: " + fileName, "FILE", fileId, fileName, request);
    }

    public void logFileDelete(String fileName, Long fileId, HttpServletRequest request) {
        String username = getCurrentUsername();
        activityLogService.logActivity(ActivityType.FILE_DELETE, username, "Obrisan fajl: " + fileName, "FILE", fileId, fileName, request);
    }

    public void logFileUpdate(String fileName, Long fileId, HttpServletRequest request) {
        String username = getCurrentUsername();
        activityLogService.logActivity(ActivityType.FILE_UPDATE, username, "Izmijenjeni metapodaci fajla: " + fileName, "FILE", fileId, fileName, request);
    }

    @SuppressWarnings("unused")
    public void logFileView(String fileName, Long fileId, HttpServletRequest request) {
        String username = getCurrentUsername();
        activityLogService.logActivity(ActivityType.FILE_VIEW, username, "Pregledao fajl: " + fileName, "FILE", fileId, fileName, request);
    }

    public void logShareCreate(String fileName, Long shareId, HttpServletRequest request) {
        String username = getCurrentUsername();
        activityLogService.logActivity(ActivityType.SHARE_CREATE, username, "Kreiran link za dijeljenje: " + fileName, "SHARE_LINK", shareId, fileName, request);
    }

    public void logShareAccess(String fileName, HttpServletRequest request) {
        activityLogService.logActivity(ActivityType.SHARE_ACCESS, "anonymous", "Pristupljeno dijeljenom fajlu: " + fileName, "SHARE_LINK", null, fileName, request);
    }

    public void logShareDelete(Long shareId, HttpServletRequest request) {
        String username = getCurrentUsername();
        activityLogService.logActivity(ActivityType.SHARE_DELETE, username, "Obrisan link za dijeljenje (ID=" + shareId + ")", "SHARE_LINK", shareId, null, request);
    }

    public void logBucketCreate(String bucketName, Long bucketId, HttpServletRequest request) {
        String username = getCurrentUsername();
        activityLogService.logActivity(ActivityType.BUCKET_CREATE, username, "Kreiran bucket: " + bucketName, "BUCKET", bucketId, bucketName, request);
    }

    public void logBucketDelete(String bucketName, HttpServletRequest request) {
        String username = getCurrentUsername();
        activityLogService.logActivity(ActivityType.BUCKET_DELETE, username, "Obrisan bucket: " + bucketName, "BUCKET", null, bucketName, request);
    }

    public void logBucketUpdate(String bucketName, Long bucketId, HttpServletRequest request) {
        String username = getCurrentUsername();
        activityLogService.logActivity(ActivityType.BUCKET_UPDATE, username, "Ažurirana polisa bucketa: " + bucketName, "BUCKET", bucketId, bucketName, request);
    }

    public void logBucketUpdate(String bucketName, Long bucketId, String message, HttpServletRequest request) {
        String username = getCurrentUsername();
        activityLogService.logActivity(ActivityType.BUCKET_UPDATE, username, message, "BUCKET", bucketId, bucketName, request);
    }

    public void logBucketSync(BucketSyncResponse syncStats, HttpServletRequest request) {
        String username = getCurrentUsername();
        String message = String.format("Sinhronizacija završena. Rezultat: %d uvezeno, %d ažurirano, %d preskočeno.",
                syncStats.getImported(), syncStats.getUpdated(), syncStats.getSkipped());
        activityLogService.logActivity(
                ActivityType.BUCKET_SYNC, username, message, "SYSTEM", null, "ALL_BUCKETS", request
        );
    }

    public void logBucketStatsRecalculation(String bucketName, HttpServletRequest request) {
        String username = getCurrentUsername();
        String message = "Izvršena rekalkulacija statistike za bucket: " + bucketName;
        activityLogService.logActivity(
                ActivityType.BUCKET_STATS_RECALCULATED, username, message, "BUCKET", null, bucketName, request
        );
    }

    public void logOrphanAudit(String bucketName, int totalOrphans, HttpServletRequest request) {
        String username = getCurrentUsername();
        String message = String.format(
                "Orphan audit završen za bucket '%s'. Pronađeno nekonzistentnosti: %d.",
                bucketName, totalOrphans);
        activityLogService.logActivity(
                ActivityType.BUCKET_ORPHAN_AUDIT, username, message, "BUCKET", null, bucketName, request
        );
    }

    @SuppressWarnings("unused")
    public void logUserCreate(String newUsername, Long newUserId, HttpServletRequest request) {
        String username = getCurrentUsername();
        activityLogService.logActivity(ActivityType.USER_CREATE, username, "Kreirao novog korisnika: " + newUsername, "USER", newUserId, newUsername, request);
    }

    public void logUserUpdate(String targetUsername, Long userId, HttpServletRequest request) {
        String username = getCurrentUsername();
        activityLogService.logActivity(ActivityType.USER_UPDATE, username, "Ažuriran korisnik: " + targetUsername, "USER", userId, targetUsername, request);
    }

    public void logUserDelete(String targetUsername, Long userId, HttpServletRequest request) {
        String username = getCurrentUsername();
        activityLogService.logActivity(ActivityType.USER_DELETE, username, "Deaktiviran korisnik: " + targetUsername, "USER", userId, targetUsername, request);
    }

    private String getCurrentUsername() {
        try {
            if (SecurityContextHolder.getContext().getAuthentication() != null &&
                    SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {

                Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (principal instanceof User) {
                    return ((User) principal).getUsername();
                }
                return principal.toString();
            }
        } catch (Exception e) {
            // Logovanje greške ovdje bi samo zatrpalo konzolu nepotrebnim informacijama
        }
        return "anonymous";
    }

}