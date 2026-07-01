package com.minio.storage.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "share_links", indexes = {
        @Index(name = "idx_share_token", columnList = "share_token"),
        @Index(name = "idx_share_file", columnList = "file_id"),
        @Index(name = "idx_share_created_by", columnList = "created_by")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ShareLink {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "share_token", nullable = false, unique = true, length = 64)
    private String shareToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "access_count", nullable = false)
    private int accessCount = 0;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "description", length = 500)
    private String description;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return isActive && !isExpired();
    }

    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
    }

    public String getTimeRemaining() {
        if (isExpired()) return "Istekao";
        long hours = Duration.between(LocalDateTime.now(), expiresAt).toHours();
        long days = hours / 24;
        if (days > 0) return days + (days == 1 ? " dan" : " dana") + " preostalo";
        if (hours > 0) return hours + (hours == 1 ? " sat" : " sati") + " preostalo";
        long minutes = Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
        return minutes + " minuta preostalo";
    }

}