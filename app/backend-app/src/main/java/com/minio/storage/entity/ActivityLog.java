package com.minio.storage.entity;

import com.minio.storage.enums.ActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs", indexes = {
        @Index(name = "idx_activity_type", columnList = "activity_type"),
        @Index(name = "idx_activity_username", columnList = "username"),
        @Index(name = "idx_activity_timestamp", columnList = "timestamp"),
        @Index(name = "idx_activity_resource", columnList = "resource_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(length = 500)
    private String description;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "resource_name")
    private String resourceName;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private boolean success = true;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public static ActivityLog success(
            ActivityType type,
            String username,
            String description,
            String resourceType,
            Long resourceId,
            String resourceName) {

        return ActivityLog.builder()
                .activityType(type)
                .username(username)
                .description(description)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .resourceName(resourceName)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ActivityLog failure(
            ActivityType type,
            String username,
            String description,
            String errorMessage) {

        return ActivityLog.builder()
                .activityType(type)
                .username(username)
                .description(description)
                .success(false)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }

}