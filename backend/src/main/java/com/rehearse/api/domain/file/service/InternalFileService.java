package com.rehearse.api.domain.file.service;

import com.rehearse.api.domain.file.dto.UpdateFileStatusRequest;
import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.file.exception.FileErrorCode;
import com.rehearse.api.domain.file.repository.FileMetadataRepository;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalFileService {

    private final FileMetadataRepository fileMetadataRepository;

    @Transactional
    public void updateFileStatus(Long fileMetadataId, UpdateFileStatusRequest request) {
        FileMetadata file = fileMetadataRepository.findById(fileMetadataId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.NOT_FOUND));

        if (request.getFailureReason() != null) {
            file.markFailed(request.getFailureReason(), request.getFailureDetail());
        } else {
            file.updateStatus(request.getStatus());
        }

        if (request.getStreamingS3Key() != null) {
            file.updateStreamingS3Key(request.getStreamingS3Key());
        }
        if (request.getFileSizeBytes() != null) {
            file.updateFileSizeBytes(request.getFileSizeBytes());
        }

        log.info("파일 상태 업데이트: fileMetadataId={}, status={}", fileMetadataId, request.getStatus());
    }

    public FileMetadata findByS3Key(String s3Key) {
        return fileMetadataRepository.findByS3Key(s3Key)
                .orElseThrow(() -> new BusinessException(FileErrorCode.S3_KEY_NOT_FOUND));
    }
}
