package com.minio.storage.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "file_versions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_file_version",
                columnNames = {"file_id", "version_number"}
        ),
        indexes = {
                @Index(name = "idx_version_file_id", columnList = "file_id"),
                @Index(name = "idx_version_minio_id", columnList = "minio_version_id"),
                @Index(name = "idx_file_version_created", columnList = "file_id, created_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FileVersion {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata fileMetadata;

    @Column(name = "minio_version_id", nullable = false, unique = true)
    private String minioVersionId;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Builder.Default
    @Column(name = "is_latest", nullable = false)
    private boolean isLatest = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

}