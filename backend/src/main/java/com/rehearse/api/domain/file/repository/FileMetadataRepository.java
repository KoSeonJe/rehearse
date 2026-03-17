package com.rehearse.api.domain.file.repository;

import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.file.entity.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    Optional<FileMetadata> findByS3Key(String s3Key);

    List<FileMetadata> findByStatusAndUpdatedAtBefore(FileStatus status, LocalDateTime before);
}
