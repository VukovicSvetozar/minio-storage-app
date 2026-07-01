package com.minio.storage.repository;

import com.minio.storage.entity.Bucket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BucketRepository extends JpaRepository<Bucket, Long> {

    boolean existsByName(String name);

    Optional<Bucket> findByIdAndDeletedAtIsNull(Long id);

    Optional<Bucket> findByNameAndDeletedAtIsNull(String name);

    long countByOwnerId(Long ownerId);

    List<Bucket> findAllByDeletedAtIsNull();

    List<Bucket> findByOwnerIdAndDeletedAtIsNull(Long ownerId);

    List<Bucket> findByIsPublicTrueAndDeletedAtIsNull();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE buckets
            SET total_size = total_size + :fileSize,
                file_count = file_count + 1
            WHERE name = :bucketName
              AND deleted_at IS NULL
              AND (total_size + :fileSize) <= max_size_bytes
            """, nativeQuery = true)
    int tryReserveSpace(
            @Param("bucketName") String bucketName,
            @Param("fileSize") long fileSize
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE buckets
            SET total_size = total_size + :size
            WHERE name = :bucketName
              AND deleted_at IS NULL
              AND (total_size + :size) <= max_size_bytes
            """, nativeQuery = true)
    int tryReserveSizeOnly(
            @Param("bucketName") String bucketName,
            @Param("size") long size
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
                UPDATE buckets
                SET total_size = total_size - :fileSize,
                    file_count = file_count - 1
                WHERE name = :bucketName
                  AND deleted_at IS NULL
                  AND total_size >= :fileSize
                  AND file_count > 0
            """, nativeQuery = true)
    int decrementStatsAfterDelete(
            @Param("bucketName") String bucketName,
            @Param("fileSize") long fileSize
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE buckets
            SET total_size = total_size - :sizeToFree
            WHERE name = :bucketName
            AND deleted_at IS NULL
            AND total_size >= :sizeToFree
            """, nativeQuery = true)
    int decrementSizeOnly(@Param("bucketName") String bucketName, @Param("sizeToFree") long sizeToFree);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bucket b WHERE b.name = :name AND b.deletedAt IS NULL")
    Optional<Bucket> findByNameForUpdate(@Param("name") String name);

    Optional<Bucket> findByName(String name);

}