package com.minio.storage.repository;

import com.minio.storage.entity.FileVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    List<FileVersion> findByFileMetadataIdOrderByVersionNumberDesc(Long fileMetadataId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM FileVersion v WHERE v.fileMetadata.id = :fileId")
    void deleteByFileId(@Param("fileId") Long fileId);

    Page<FileVersion> findByFileMetadataId(Long fileMetadataId, Pageable pageable);

    Optional<FileVersion> findByIdAndFileMetadataId(Long id, Long fileMetadataId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FileVersion v SET v.isLatest = false WHERE v.fileMetadata.id = :fileId")
    void clearLatestFlag(@Param("fileId") Long fileId);

    @Query("SELECT MAX(v.versionNumber) FROM FileVersion v WHERE v.fileMetadata.id = :fileId")
    Optional<Integer> findMaxVersionNumber(@Param("fileId") Long fileId);

    long countByFileMetadataId(Long fileMetadataId);

    Optional<FileVersion> findTopByFileMetadataIdOrderByVersionNumberDesc(Long fileId);

}