package com.minio.storage.service;

import com.minio.storage.dto.LifecycleConfigRequest;
import com.minio.storage.dto.LifecycleConfigResponse;
import com.minio.storage.dto.RetentionPolicyRequest;
import com.minio.storage.dto.RetentionPolicyResponse;
import com.minio.storage.exception.AppException;
import com.minio.storage.repository.BucketRepository;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BucketLifecycleService {

    private static final String MINIO_NO_SUCH_LIFECYCLE = "NoSuchLifecycleConfiguration";
    private static final String MINIO_OBJECT_LOCK_NOT_ENABLED = "ObjectLockConfigurationNotFoundError";
    private final MinioClient minioClient;
    private final BucketRepository bucketRepository;

    @Transactional(readOnly = true)
    public LifecycleConfigResponse getLifecycleConfig(String bucketName) {
        validateBucketExists(bucketName);

        try {
            LifecycleConfiguration config = minioClient.getBucketLifecycle(
                    GetBucketLifecycleArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            List<LifecycleConfigResponse.LifecycleRuleResponse> rules = new ArrayList<>();

            if (config != null && config.rules() != null) {
                for (LifecycleRule rule : config.rules()) {
                    rules.add(LifecycleConfigResponse.LifecycleRuleResponse.builder()
                            .id(rule.id())
                            .status(rule.status() != null ? rule.status().toString() : "ENABLED")
                            .filterPrefix(rule.filter() != null &&
                                    rule.filter().prefix() != null
                                    ? rule.filter().prefix() : null)
                            .expirationDays(getExpirationDays(rule))
                            .transitionDays(getTransitionDays(rule))
                            .transitionBucket(getTransitionBucket(rule))
                            .build());
                }
            }

            log.debug("Dohvaćena lifecycle konfiguracija za bucket: {}, pravila: {}",
                    bucketName, rules.size());

            return LifecycleConfigResponse.builder()
                    .bucketName(bucketName)
                    .ruleCount(rules.size())
                    .rules(rules)
                    .build();

        } catch (ErrorResponseException e) {
            if (MINIO_NO_SUCH_LIFECYCLE.equals(e.errorResponse().code())) {
                log.info("Lifecycle nije postavljen za bucket: {}", bucketName);
                return emptyResponse(bucketName);
            }
            log.error("Greška iz MinIO pri dohvatanju lifecycle za bucket: {}", bucketName, e);
            throw AppException.internalError("Greška pri komunikaciji sa storage sistemom.");
        } catch (Exception e) {
            log.error("Neočekivana greška pri dohvatanju lifecycle za bucket: {}", bucketName, e);
            throw AppException.internalError("Greška pri komunikaciji sa storage sistemom.");
        }
    }

    public LifecycleConfigResponse setLifecycleConfig(String bucketName, LifecycleConfigRequest request) {
        validateBucketExists(bucketName);

        try {
            List<LifecycleRule> minioRules = request.getRules().stream()
                    .map(this::mapToMinioRule)
                    .collect(Collectors.toList());

            minioClient.setBucketLifecycle(
                    SetBucketLifecycleArgs.builder()
                            .bucket(bucketName)
                            .config(new LifecycleConfiguration(minioRules))
                            .build()
            );

            log.info("Lifecycle konfiguracija postavljena za bucket: {}, pravila: {}", bucketName, minioRules.size());

            return getLifecycleConfig(bucketName);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Greška pri postavljanju lifecycle konfiguracije za bucket {}", bucketName, e);
            throw AppException.storageError("Nije moguće postaviti lifecycle konfiguraciju.");
        }
    }

    public void deleteLifecycleConfig(String bucketName) {
        validateBucketExists(bucketName);

        try {
            minioClient.deleteBucketLifecycle(
                    DeleteBucketLifecycleArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            log.info("Lifecycle konfiguracija obrisana za bucket: {}", bucketName);

        } catch (ErrorResponseException e) {
            if (MINIO_NO_SUCH_LIFECYCLE.equals(e.errorResponse().code())) {
                log.info("Lifecycle konfiguracija već ne postoji za bucket: {}", bucketName);
                return;
            }
            log.error("Greška pri brisanju lifecycle konfiguracije za bucket: {}", bucketName, e);
            throw AppException.storageError("Greška pri komunikaciji sa storage sistemom.");
        } catch (Exception e) {
            log.error("Greška pri brisanju lifecycle konfiguracije za bucket: {}", bucketName, e);
            throw AppException.storageError("Greška pri komunikaciji sa storage sistemom.");
        }
    }

    @Transactional(readOnly = true)
    public RetentionPolicyResponse getRetentionPolicy(String bucketName) {
        validateBucketExists(bucketName);

        try {
            ObjectLockConfiguration config = minioClient.getObjectLockConfiguration(
                    GetObjectLockConfigurationArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            String mode = config.mode() != null ? config.mode().toString() : null;
            Integer days = config.duration() != null ? config.duration().duration() : null;

            String note = buildRetentionNote(mode);
            if (mode == null) {
                note = "Object Lock je omogućen, ali trenutno nema postavljenog podrazumijevanog pravila zadržavanja.";
            }
            log.debug("Dohvaćen retention policy za bucket: {}, mod: {}", bucketName, mode);

            return RetentionPolicyResponse.builder()
                    .bucketName(bucketName)
                    .objectLockEnabled(true)
                    .mode(mode)
                    .days(days)
                    .note(note)
                    .build();
        } catch (ErrorResponseException e) {
            if (MINIO_OBJECT_LOCK_NOT_ENABLED.equals(e.errorResponse().code())) {
                log.debug("Object Lock nije uključen za bucket: {}", bucketName);
                return RetentionPolicyResponse.builder()
                        .bucketName(bucketName)
                        .objectLockEnabled(false)
                        .mode(null)
                        .days(null)
                        .note("Object Lock nije uključen na ovom bucketu. " +
                                "Retention policy zahtijeva bucket kreiran sa object-lock=true.")
                        .build();
            }
            log.error("Greška iz MinIO pri dohvatanju retention policy za bucket: {}", bucketName, e);
            throw AppException.storageError("Greška pri komunikaciji sa storage sistemom.");
        } catch (Exception e) {
            log.error("Neočekivana greška pri dohvatanju retention policy za bucket: {}", bucketName, e);
            throw AppException.internalError("Greška pri komunikaciji sa storage sistemom.");
        }
    }

    @Transactional
    public RetentionPolicyResponse setRetentionPolicy(String bucketName,
                                                      RetentionPolicyRequest request) {
        validateBucketExists(bucketName);
        RetentionMode retentionMode;
        try {
            retentionMode = RetentionMode.valueOf(request.getMode().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest(
                    "Neispravni mod: " + request.getMode() +
                            ". Dozvoljene vrijednosti su COMPLIANCE i GOVERNANCE.");
        }
        try {
            minioClient.setObjectLockConfiguration(
                    SetObjectLockConfigurationArgs.builder()
                            .bucket(bucketName)
                            .config(new ObjectLockConfiguration(
                                    retentionMode,
                                    new RetentionDurationDays(request.getDays())
                            ))
                            .build()
            );
            log.info("Retention policy postavljen za bucket: {}, mod: {}, dana: {}",
                    bucketName, retentionMode, request.getDays());
            return getRetentionPolicy(bucketName);
        } catch (AppException e) {
            throw e;
        } catch (ErrorResponseException e) {
            if (MINIO_OBJECT_LOCK_NOT_ENABLED.equals(e.errorResponse().code())) {
                log.warn("Pokušaj postavljanja retention policy na bucket bez Object Lock: {}", bucketName);
                throw AppException.conflict(
                        "Bucket nije kreiran sa Object Lock podrškom. " +
                                "Retention policy zahtijeva bucket kreiran sa object-lock=true.");
            }
            log.error("Greška iz MinIO pri postavljanju retention policy za bucket: {}", bucketName, e);
            throw AppException.storageError("Greška pri komunikaciji sa storage sistemom.");
        } catch (Exception e) {
            log.error("Neočekivana greška pri postavljanju retention policy za bucket: {}", bucketName, e);
            throw AppException.internalError("Greška pri komunikaciji sa storage sistemom.");
        }
    }

    private void validateBucketExists(String bucketName) {
        bucketRepository.findByNameAndDeletedAtIsNull(bucketName)
                .orElseThrow(() -> AppException.notFound(
                        "Bucket '" + bucketName + "' nije pronađen."));
    }

    private Integer getExpirationDays(LifecycleRule rule) {
        if (rule.expiration() == null || rule.expiration().days() == null) return null;
        return rule.expiration().days();
    }

    private Integer getTransitionDays(LifecycleRule rule) {
        if (rule.transition() == null || rule.transition().days() == null) return null;
        return rule.transition().days();
    }

    private String getTransitionBucket(LifecycleRule rule) {
        if (rule.transition() == null || rule.transition().storageClass() == null) return null;
        return rule.transition().storageClass();
    }

    private LifecycleConfigResponse emptyResponse(String bucketName) {
        return LifecycleConfigResponse.builder()
                .bucketName(bucketName)
                .ruleCount(0)
                .rules(List.of())
                .build();
    }

    @SuppressWarnings("ConstantConditions")
    private LifecycleRule mapToMinioRule(LifecycleConfigRequest.LifecycleRule rule) {
        validateRule(rule);

        RuleFilter filter = null;
        if (rule.getFilter() != null && rule.getFilter().getPrefix() != null) {
            filter = new RuleFilter(rule.getFilter().getPrefix());
        }

        Expiration expiration = null;
        if (rule.getExpiration() != null && rule.getExpiration().getDays() != null) {
            expiration = new Expiration(
                    (ZonedDateTime) null,
                    rule.getExpiration().getDays(),
                    rule.getExpiration().isExpiredObjectDeleteMarker()
            );
        }

        Transition transition = null;
        if (rule.getTransition() != null && rule.getTransition().getDays() != null) {
            transition = new Transition(
                    (ZonedDateTime) null,
                    rule.getTransition().getDays(),
                    rule.getTransition().getTargetBucket()
            );
        }

        return new LifecycleRule(
                rule.isEnabled() ? Status.ENABLED : Status.DISABLED,
                null,
                expiration,
                filter,
                rule.getId(),
                null,
                null,
                transition
        );
    }

    private void validateRule(LifecycleConfigRequest.LifecycleRule rule) {
        if (rule.getExpiration() == null && rule.getTransition() == null) {
            throw AppException.badRequest("Pravilo '" + rule.getId() + "' mora imati expiration ili transition definisan.");
        }
    }

    private String buildRetentionNote(String mode) {
        if (mode == null)
            return null;
        if ("COMPLIANCE".equalsIgnoreCase(mode)) {
            return "COMPLIANCE režim - Stroga zaštita. Objekti se ne mogu brisati niti mijenjati " +
                    "do isteka definisanog perioda. Ovu zaštitu ne može poništiti čak ni administrator sistema.";
        }
        if ("GOVERNANCE".equalsIgnoreCase(mode)) {
            return "GOVERNANCE režim - Fleksibilna zaštita. Objekti su zaštićeni od brisanja, " +
                    "ali korisnici sa posebnim privilegijama (administratori) mogu zaobići ili ukloniti zaštitu.";
        }
        return null;
    }

}