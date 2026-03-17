package com.rehearse.api.domain.file.dto;

import com.rehearse.api.domain.file.entity.FileStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateFileStatusRequest {

    @NotNull(message = "파일 상태는 필수입니다.")
    private FileStatus status;

    private String streamingS3Key;
    private Long fileSizeBytes;
    private String failureReason;
    private String failureDetail;
}
