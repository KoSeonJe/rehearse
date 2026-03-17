package com.rehearse.api.domain.file.dto;

import com.rehearse.api.domain.file.entity.FileMetadata;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileMetadataResponse {

    private final Long id;
    private final String fileType;
    private final String status;
    private final String s3Key;
    private final String streamingS3Key;
    private final String bucket;

    public static FileMetadataResponse from(FileMetadata file) {
        return FileMetadataResponse.builder()
                .id(file.getId())
                .fileType(file.getFileType().name())
                .status(file.getStatus().name())
                .s3Key(file.getS3Key())
                .streamingS3Key(file.getStreamingS3Key())
                .bucket(file.getBucket())
                .build();
    }
}
