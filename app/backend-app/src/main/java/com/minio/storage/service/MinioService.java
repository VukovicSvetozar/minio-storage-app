package com.minio.storage.service;

import com.minio.storage.entity.FileVersion;
import com.minio.storage.enums.MinioReachabilityStatus;
import com.minio.storage.exception.AppException;
import com.minio.storage.util.TxCleanup;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import io.minio.messages.VersioningConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
        } catch (Exception e) {
            log.error("MinIO: greška pri provjeri postojanja bucketa '{}'", bucketName, e);
            throw AppException.storageError("Servis za skladištenje trenutno nije dostupan.");
        }
    }

    public void createBucket(String bucketName, boolean objectLock) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (exists) {
                throw AppException.conflict("Bucket: '" + bucketName + "' već postoji na serveru.");
            }

            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .objectLock(objectLock)
                            .build()
            );

            log.info("MinIO: Kreiran bucket '{}', objectLock={}", bucketName, objectLock);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("MinIO: Greška pri kreiranju bucketa '{}'", bucketName, e);
            throw AppException.storageError("Greška pri kreiranju prostora za skladištenje.");
        }
    }

    public void deleteBucket(String bucketName) {

        try {

            if (bucketHasObjects(bucketName)) {
                throw AppException.badRequest("Bucket '" + bucketName + "' nije prazan. Brisanje nije dozvoljeno.");
            }

            minioClient.removeBucket(
                    RemoveBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            log.info("MinIO: Bucket '{}' uspješno uklonjen (obrisan sa servera i označen kao obrisan u bazi)", bucketName);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("MinIO: Greška pri brisanju bucketa '{}'", bucketName, e);
            throw AppException.storageError("Greška pri brisanju prostora na serveru za skladištenje.");
        }
    }

    public void setBucketPolicy(String bucketName, boolean isPublic) {
        try {
            if (isPublic) {
                String policy = """
                        {
                          "Version": "2012-10-17",
                          "Statement": [
                            {
                              "Effect": "Allow",
                              "Principal": {"AWS": "*"},
                              "Action": ["s3:GetObject"],
                              "Resource": ["arn:aws:s3:::%s/*"]
                            }
                          ]
                        }
                        """.formatted(bucketName);

                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(bucketName)
                                .config(policy)
                                .build()
                );
                log.info("MinIO: Polisa bucketa '{}' postavljena na PUBLIC", bucketName);
            } else {
                minioClient.deleteBucketPolicy(
                        DeleteBucketPolicyArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("MinIO: Polisa bucketa '{}' obrisana (PRIVATE)", bucketName);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("MinIO: Greška pri postavljanju polise za bucket '{}'", bucketName, e);
            throw AppException.storageError("Greška pri izmjeni prava pristupa na serveru.");
        }
    }

    public void removeObject(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("MinIO: Uspješno obrisan objekat: {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("MinIO: Greška pri brisanju objekta '{}/{}'", bucketName, objectName, e);
            throw AppException.internalError("Greška prilikom brisanja fajla sa servera.");
        }
    }

    public void removeObject(String bucketName, String objectName, String versionId) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .versionId(versionId)
                            .build()
            );
            log.info("MinIO: Obrisana verzija objekta: {}/{} (versionId={})",
                    bucketName, objectName, versionId);
        } catch (Exception e) {
            log.error("MinIO: Greška pri brisanju verzije '{}/{}' (versionId={})",
                    bucketName, objectName, versionId, e);
            throw AppException.internalError("Greška prilikom brisanja verzije fajla sa servera.");
        }
    }

    public List<String> removeObjects(String bucketName, List<FileVersion> versions) {

        List<DeleteObject> objects = versions.stream()
                .map(v -> new DeleteObject(v.getStorageKey(), v.getMinioVersionId()))
                .toList();

        List<String> failed = new ArrayList<>();

        Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucketName)
                        .objects(objects)
                        .build()
        );

        for (Result<DeleteError> result : results) {
            try {
                DeleteError error = result.get();
                failed.add(error.objectName());
                log.error("BATCH_DELETE_ERROR: Nije moguće obrisati objekat {}/{}. Ručno brisanje potrebno!", bucketName, error.objectName());
            } catch (Exception e) {
                log.error("ROLLBACK: Greška pri batch brisanju iz MinIO za bucket={}",
                        bucketName, e);
            }
        }

        return failed;
    }

    public String putObject(String bucketName, String objectName,
                            InputStream inputStream, long size, String contentType) {
        try {
            ObjectWriteResponse response = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );
            log.info("MinIO: Uspješno otpremljen objekat: {}/{}", bucketName, objectName);
            return response.versionId();
        } catch (Exception e) {
            log.error("MinIO: Greška pri otpremanju objekta '{}/{}'", bucketName, objectName, e);
            throw AppException.internalError("Greška prilikom slanja fajla na server.");
        }
    }

    public void registerRollbackCleanup(String bucketName, String objectName, String versionId) {
        if (versionId != null) {
            TxCleanup.onRollback(() -> {
                try {
                    removeObject(bucketName, objectName, versionId);
                } catch (Exception e) {
                    log.error("ROLLBACK: Nije moguće obrisati verziju {}/{} (versionId={}). " +
                            "Ručno brisanje potrebno!", bucketName, objectName, versionId, e);
                }
            });
        } else {
            TxCleanup.onRollback(() -> {
                try {
                    removeObject(bucketName, objectName);
                } catch (Exception e) {
                    log.error("ROLLBACK: Nije moguće obrisati objekat {}/{}. " +
                            "Ručno brisanje potrebno!", bucketName, objectName, e);
                }
            });
        }
    }

    public List<String> listObjects(String bucketName) {
        List<String> objects = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .recursive(true)
                            .build()
            );
            for (Result<Item> result : results) {
                try {
                    objects.add(result.get().objectName());
                } catch (Exception e) {
                    log.warn("Greška pri čitanju objekta iz bucketa {}: {}", bucketName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Greška pri listanju objekata u bucketu {}: {}", bucketName, e.getMessage());
            throw AppException.storageError("Greška pri listanju objekata.");
        }
        return objects;
    }

    public String getPresignedUrl(String bucketName, String objectName,
                                  int expirationSeconds, String contentDisposition) {
        return getPresignedUrlInternal(bucketName, objectName, null, expirationSeconds, contentDisposition);
    }

    public String getPresignedUrlForVersion(String bucketName, String objectName,
                                            String versionId, int expirationSeconds,
                                            String contentDisposition) {
        return getPresignedUrlInternal(bucketName, objectName, versionId, expirationSeconds, contentDisposition);
    }

    public InputStream getObject(String bucketName, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO: Greška pri preuzimanju objekta '{}/{}'", bucketName, objectName, e);
            throw AppException.internalError("Neuspješno preuzimanje fajla sa skladišta.");
        }
    }

    public String copyObject(String bucketName, String objectName, String sourceVersionId) {
        try {
            CopySource source = CopySource.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .versionId(sourceVersionId)
                    .build();

            ObjectWriteResponse response = minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .source(source)
                            .build()
            );

            log.info("MinIO: Uspješan restore (copy) za objekat {}/{}", bucketName, objectName);
            return response.versionId();

        } catch (Exception e) {
            log.error("MinIO: Greška pri kopiranju objekta '{}/{}' sa verzijom {}", bucketName, objectName, sourceVersionId, e);
            throw AppException.internalError("Sistemska greška prilikom vraćanja verzije fajla.");
        }
    }

    public VersioningConfiguration.Status getBucketVersioningStatus(String bucketName) {
        try {
            VersioningConfiguration config = minioClient.getBucketVersioning(
                    GetBucketVersioningArgs.builder().bucket(bucketName).build()
            );

            return (config != null && config.status() != null)
                    ? config.status()
                    : VersioningConfiguration.Status.OFF;

        } catch (Exception e) {
            log.error("MinIO: Greška pri dohvatanju statusa verzioniranja za bucket '{}'", bucketName, e);
            throw AppException.storageError("Nije moguće provjeriti status verzioniranja na sistemu.");
        }
    }

    public void setBucketVersioning(String bucketName, boolean enable) {
        try {
            VersioningConfiguration.Status status = enable ?
                    VersioningConfiguration.Status.ENABLED :
                    VersioningConfiguration.Status.SUSPENDED;

            minioClient.setBucketVersioning(
                    SetBucketVersioningArgs.builder()
                            .bucket(bucketName)
                            .config(new VersioningConfiguration(status, null))
                            .build()
            );
            log.info("MinIO: Konfiguracija verzioniranja za '{}' postavljena na {}", bucketName, status);
        } catch (Exception e) {
            log.error("MinIO error: Neuspješno podešavanje verzioniranja za bucket '{}'", bucketName, e);
            throw AppException.storageError("Storage server nije prihvatio promjenu statusa verzioniranja.");
        }
    }

    public List<String> listAllBuckets() {
        try {
            return minioClient.listBuckets().stream()
                    .map(io.minio.messages.Bucket::name)
                    .toList();
        } catch (Exception e) {
            log.error("MinIO: Greška pri listanju bucketa", e);
            throw AppException.storageError("Nije moguće izlistati buckete.");
        }
    }

    public boolean isBucketPublic(String bucketName) {
        try {
            String policy = minioClient.getBucketPolicy(
                    GetBucketPolicyArgs.builder().bucket(bucketName).build()
            );
            return policy != null
                    && policy.contains("\"Effect\": \"Allow\"")
                    && policy.contains("s3:GetObject");
        } catch (Exception e) {
            log.warn("MinIO: Neuspješno čitanje polise za bucket '{}'", bucketName);
            return false;
        }
    }

    public MinioReachabilityStatus checkBucketReachability(String bucketName) {
        try {
            minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            return MinioReachabilityStatus.REACHABLE;
        } catch (ErrorResponseException e) {
            String code = e.errorResponse().code();
            log.warn("MinIO: Error response za bucket '{}': {}", bucketName, code);
            if ("AccessDenied".equals(code) || "Forbidden".equals(code)) {
                return MinioReachabilityStatus.PERMISSION_DENIED;
            }
            return MinioReachabilityStatus.NETWORK_ERROR;
        } catch (Exception e) {
            log.warn("MinIO: Server nije dostupan za bucket '{}'", bucketName, e);
            return MinioReachabilityStatus.NETWORK_ERROR;
        }
    }

    private String getPresignedUrlInternal(String bucketName, String objectName,
                                           String versionId, int expirationSeconds,
                                           String contentDisposition) {
        try {
            Map<String, String> reqParams = new HashMap<>();
            if (contentDisposition != null) {
                reqParams.put("response-content-disposition", contentDisposition);
            }

            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(expirationSeconds, TimeUnit.SECONDS)
                    .extraQueryParams(reqParams);

            if (versionId != null) {
                builder.versionId(versionId);
            }

            return minioClient.getPresignedObjectUrl(builder.build());

        } catch (Exception e) {
            log.error("MinIO: Greška pri generisanju presigned URL-a za '{}/{}'", bucketName, objectName, e);
            throw AppException.internalError("Nije moguće generisati link za pristup fajlu.");
        }
    }

    private boolean bucketHasObjects(String bucketName) {
        try {
            for (Result<Item> result : minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .maxKeys(1)
                            .build())) {
                result.get();
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("MinIO: Greška pri provjeri sadržaja bucketa '{}'", bucketName, e);
            throw AppException.storageError("Nije moguće pročitati sadržaj bucketa.");
        }
    }

}