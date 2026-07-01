package com.minio.storage.entity;

import com.minio.storage.enums.FileCategory;
import com.minio.storage.util.FormatUtils;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Arrays;

@Entity
@Table(name = "file_metadata", indexes = {
        @Index(name = "idx_file_bucket", columnList = "bucket_name"),
        @Index(name = "idx_file_owner", columnList = "owner_id"),
        @Index(name = "idx_file_owner_category", columnList = "owner_id, category"),
        @Index(name = "idx_file_owner_bucket", columnList = "owner_id, bucket_name"),
        @Index(name = "idx_file_public", columnList = "is_public"),
        @Index(name = "idx_file_category", columnList = "category"),
        @Index(name = "idx_file_uploaded", columnList = "uploaded_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FileMetadata {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "object_name", nullable = false, unique = true)
    private String objectName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "bucket_name", nullable = false, length = 63)
    private String bucketName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private FileCategory category;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "tags", length = 500)
    private String tags;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getExtension() {
        if (originalFileName == null || !originalFileName.contains(".")) {
            return "";
        }
        return originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
    }

    public String[] getTagsArray() {
        return (tags != null && !tags.isEmpty())
                ? Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .toArray(String[]::new)
                : new String[0];
    }

    public String getOwnerUsername() {
        return owner != null ? owner.getUsername() : "Nepoznat korisnik";
    }

    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    public String getFormattedSize() {
        return FormatUtils.formatFileSize(fileSize);
    }

}