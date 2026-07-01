package com.minio.storage.repository;

import com.minio.storage.entity.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    Optional<ShareLink> findByShareToken(String shareToken);

    List<ShareLink> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    @Query("""
            SELECT s FROM ShareLink s
            WHERE s.createdBy = :createdBy
              AND s.isActive = true
              AND s.expiresAt > :now
            ORDER BY s.createdAt DESC
            """)
    List<ShareLink> findValidByCreatedBy(
            @Param("createdBy") String createdBy,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Query("DELETE FROM ShareLink s WHERE s.file.id = :fileId")
    void deleteByFileId(@Param("fileId") Long fileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                UPDATE ShareLink s
                SET s.isActive = false
                WHERE s.createdBy = :username
            """)
    void deactivateAllByCreatedBy(@Param("username") String username);

}