package com.minio.storage.repository;

import com.minio.storage.entity.FileMetadata;
import com.minio.storage.enums.FileCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository
        extends JpaRepository<FileMetadata, Long>,
        JpaSpecificationExecutor<FileMetadata> {

    @Query("""
                SELECT f FROM FileMetadata f
                JOIN Bucket b ON b.name = f.bucketName
                WHERE b.deletedAt IS NULL
                  AND f.owner.id = :ownerId
            """)
    Page<FileMetadata> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("""
                SELECT f FROM FileMetadata f
                JOIN Bucket b ON b.name = f.bucketName
                WHERE b.deletedAt IS NULL
                  AND (f.isPublic = true OR b.isPublic = true)
            """)
    Page<FileMetadata> findPublicFiles(Pageable pageable);

    @Query("""
                SELECT f FROM FileMetadata f
                JOIN Bucket b ON b.name = f.bucketName
                WHERE b.deletedAt IS NULL
                  AND f.category = :category
            """)
    Page<FileMetadata> findByCategoryForAdmin(
            @Param("category") FileCategory category,
            Pageable pageable
    );

    @Query("""
                SELECT f FROM FileMetadata f
                JOIN Bucket b ON b.name = f.bucketName
                WHERE b.deletedAt IS NULL
                  AND f.category = :category
                  AND (
                        f.isPublic = true
                     OR b.isPublic = true
                     OR f.owner.id = :userId
                     OR b.owner.id = :userId
                  )
            """)
    Page<FileMetadata> findByCategoryAndAccessibleByUser(
            @Param("category") FileCategory category,
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
                SELECT f FROM FileMetadata f
                JOIN Bucket b ON b.name = f.bucketName
                WHERE b.deletedAt IS NULL
                  AND f.category = :category
                  AND (f.isPublic = true OR b.isPublic = true)
            """)
    Page<FileMetadata> findByCategoryAndPublic(
            @Param("category") FileCategory category,
            Pageable pageable
    );

    @Query("""
                SELECT f FROM FileMetadata f
                JOIN Bucket b ON b.name = f.bucketName
                WHERE b.deletedAt IS NULL
                  AND b.name = :bucketName
            """)
    Page<FileMetadata> findByBucketName(
            @Param("bucketName") String bucketName,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(f), COALESCE(SUM(f.fileSize), 0)
            FROM FileMetadata f
            JOIN Bucket b ON b.name = f.bucketName
            WHERE f.owner.id = :userId
              AND b.deletedAt IS NULL
            """)
    Object[] getCountAndSizeByUser(@Param("userId") Long userId);

    @Query("""
            SELECT f.category, COUNT(f)
            FROM FileMetadata f
            JOIN Bucket b ON b.name = f.bucketName
            WHERE f.owner.id = :userId
              AND b.deletedAt IS NULL
            GROUP BY f.category
            """)
    List<Object[]> getCategoryStatsByUser(@Param("userId") Long userId);

    @Query("""
            SELECT f.bucketName, COUNT(f)
            FROM FileMetadata f
            JOIN Bucket b ON b.name = f.bucketName
            WHERE f.owner.id = :userId
              AND b.deletedAt IS NULL
            GROUP BY f.bucketName
            """)
    List<Object[]> getBucketStatsByUser(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(f)
            FROM FileMetadata f
            JOIN Bucket b ON f.bucketName = b.name
            WHERE b.deletedAt IS NULL
            """)
    long countAllActiveFiles();

    @Query("""
            SELECT COALESCE(SUM(f.fileSize), 0)
            FROM FileMetadata f
            JOIN Bucket b ON f.bucketName = b.name
            WHERE b.deletedAt IS NULL
            """)
    long sumAllActiveFileSizes();

    @Query("""
            SELECT f.category, COUNT(f)
            FROM FileMetadata f
            JOIN Bucket b ON f.bucketName = b.name
            WHERE b.deletedAt IS NULL
            GROUP BY f.category
            """)
    List<Object[]> countByCategory();

    @Query("""
            SELECT f.owner.username, COALESCE(SUM(f.fileSize), 0)
            FROM FileMetadata f
            JOIN Bucket b ON f.bucketName = b.name
            WHERE b.deletedAt IS NULL
            GROUP BY f.owner.username
            """)
    List<Object[]> getTotalStorageByUsers();

    @Query("""
            SELECT f.bucketName, COUNT(f)
            FROM FileMetadata f
            JOIN Bucket b ON f.bucketName = b.name
            WHERE b.deletedAt IS NULL
            GROUP BY f.bucketName
            """)
    List<Object[]> countFilesByBucket();

    @Query("""
            SELECT f.owner.username, COUNT(f), COALESCE(SUM(f.fileSize), 0) AS totalSize
            FROM FileMetadata f
            JOIN Bucket b ON f.bucketName = b.name
            WHERE b.deletedAt IS NULL
            GROUP BY f.owner.id, f.owner.username
            ORDER BY totalSize DESC
            """)
    List<Object[]> getUserStorageStatsComplete(Pageable pageable);

    @Query("""
            SELECT f.category, COUNT(f), COALESCE(SUM(f.fileSize), 0)
            FROM FileMetadata f
            JOIN Bucket b ON b.name = f.bucketName
            WHERE b.deletedAt IS NULL
            GROUP BY f.category
            ORDER BY f.category ASC
            """)
    List<Object[]> getCategoryStatsComplete();

    @Query("""
            SELECT f.bucketName, COUNT(f), COALESCE(SUM(f.fileSize), 0)
            FROM FileMetadata f
            JOIN Bucket b ON f.bucketName = b.name
            WHERE b.deletedAt IS NULL
            GROUP BY f.bucketName
            ORDER BY SUM(f.fileSize) DESC
            """)
    List<Object[]> getBucketStorageStats(Pageable pageable);

    @Query("""
            SELECT COUNT(f), COALESCE(SUM(f.fileSize), 0)
            FROM FileMetadata f
            JOIN Bucket b ON b.name = f.bucketName
            WHERE f.bucketName = :bucketName
              AND b.deletedAt IS NULL
            """)
    Object[] getBucketStatistics(@Param("bucketName") String bucketName);

    @Query("SELECT f.objectName FROM FileMetadata f WHERE f.bucketName = :bucketName")
    List<String> findAllObjectNamesByBucketName(@Param("bucketName") String bucketName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FileMetadata f WHERE f.id = :id")
    Optional<FileMetadata> findByIdWithLock(@Param("id") Long id);

}