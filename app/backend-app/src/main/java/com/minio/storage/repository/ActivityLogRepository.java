package com.minio.storage.repository;

import com.minio.storage.entity.ActivityLog;
import com.minio.storage.enums.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findByUsername(String username, Pageable pageable);

    @SuppressWarnings("unused")
    List<ActivityLog> findByUsername(String username);

    @SuppressWarnings("unused")
    Page<ActivityLog> findByActivityType(ActivityType activityType, Pageable pageable);

    List<ActivityLog> findByResourceId(Long resourceId);

    @SuppressWarnings("unused")
    Page<ActivityLog> findByTimestampBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable);

    @SuppressWarnings("unused")
    Page<ActivityLog> findByUsernameAndActivityType(
            String username,
            ActivityType activityType,
            Pageable pageable);

    @SuppressWarnings("unused")
    @Query("SELECT a FROM ActivityLog a WHERE a.username = :username " +
            "AND a.timestamp BETWEEN :start AND :end")
    Page<ActivityLog> findByUsernameAndDateRange(
            @Param("username") String username,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    List<ActivityLog> findTop10ByOrderByTimestampDesc();

    @SuppressWarnings("unused")
    long countByUsername(String username);

    long countByActivityType(ActivityType activityType);

    @SuppressWarnings("unused")
    Page<ActivityLog> findBySuccess(Boolean success, Pageable pageable);

    long countByTimestampAfter(LocalDateTime timestamp);

    long countBySuccess(Boolean success);

    @Query(value = "SELECT a FROM ActivityLog a WHERE " +
            "(:username IS NULL OR a.username = :username) AND " +
            "(:activityType IS NULL OR a.activityType = :activityType) AND " +
            "(:resourceType IS NULL OR a.resourceType = :resourceType) AND " +
            "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
            "(:endDate IS NULL OR a.timestamp <= :endDate) AND " +
            "(:success IS NULL OR a.success = :success)",
            countQuery = "SELECT count(a) FROM ActivityLog a WHERE " +
                    "(:username IS NULL OR a.username = :username) AND " +
                    "(:activityType IS NULL OR a.activityType = :activityType) AND " +
                    "(:resourceType IS NULL OR a.resourceType = :resourceType) AND " +
                    "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
                    "(:endDate IS NULL OR a.timestamp <= :endDate) AND " +
                    "(:success IS NULL OR a.success = :success)")
    Page<ActivityLog> findByFilters(
            @Param("username") String username,
            @Param("activityType") ActivityType activityType,
            @Param("resourceType") String resourceType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("success") Boolean success,
            Pageable pageable);

}