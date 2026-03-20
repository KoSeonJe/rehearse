package com.rehearse.api.domain.file.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_metadata")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileType fileType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileStatus status;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "streaming_s3_key", length = 500)
    private String streamingS3Key;

    @Column(length = 100)
    private String bucket;

    @Column(length = 100)
    private String contentType;

    private Long fileSizeBytes;

    @Column(length = 500)
    private String failureReason;

    @Column(columnDefinition = "TEXT")
    private String failureDetail;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @Builder
    public FileMetadata(FileType fileType, String s3Key, String bucket, String contentType) {
        this.fileType = fileType;
        this.status = FileStatus.PENDING;
        this.s3Key = s3Key;
        this.bucket = bucket;
        this.contentType = contentType;
    }

    public void updateStatus(FileStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("파일 상태를 %s에서 %s로 변경할 수 없습니다.", this.status, newStatus));
        }
        this.status = newStatus;
    }

    public void markFailed(String reason, String detail) {
        updateStatus(FileStatus.FAILED);
        this.failureReason = reason;
        this.failureDetail = detail;
    }

    public void updateStreamingS3Key(String streamingS3Key) {
        this.streamingS3Key = streamingS3Key;
    }

    public void updateFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }
}
