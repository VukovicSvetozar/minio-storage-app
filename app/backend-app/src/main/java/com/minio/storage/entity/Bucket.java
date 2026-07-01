package com.minio.storage.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "buckets",
        indexes = {
                @Index(name = "idx_bucket_owner", columnList = "owner_id"),
                @Index(name = "idx_bucket_public", columnList = "is_public"),
                @Index(name = "idx_bucket_deleted", columnList = "deleted_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Bucket {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true, length = 63)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(name = "allow_public_upload", nullable = false)
    private boolean allowPublicUpload = false;

    @Column(name = "max_size_bytes", nullable = false)
    private Long maxSizeBytes;

    @Getter
    @Setter(AccessLevel.NONE)
    @Column(name = "total_size", nullable = false)
    private Long totalSize = 0L;

    @Getter
    @Setter(AccessLevel.NONE)
    @Column(name = "file_count", nullable = false)
    private Long fileCount = 0L;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "versioning_enabled", nullable = false)
    @Builder.Default
    private boolean versioningEnabled = false;

    @Column(name = "object_lock_enabled", nullable = false)
    @Builder.Default
    private boolean objectLockEnabled = false;

    public Bucket(String name, User owner, boolean isPublic, Long maxSizeBytes) {
        this.name = name;
        this.owner = owner;
        this.isPublic = isPublic;
        this.maxSizeBytes = maxSizeBytes;
        this.totalSize = 0L;
        this.fileCount = 0L;
    }

    public String getOwnerUsername() {
        return (owner != null) ? owner.getUsername() : "Nepoznat korisnik";
    }

    public boolean isActive() {
        return this.deletedAt == null;
    }

    public void syncStatistics(Long fileCount, Long totalSize) {
        this.fileCount = fileCount;
        this.totalSize = totalSize;
        this.lastSyncAt = LocalDateTime.now();
    }

}