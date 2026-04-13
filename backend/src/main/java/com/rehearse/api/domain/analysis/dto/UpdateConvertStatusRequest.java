package com.rehearse.api.domain.analysis.dto;

import com.rehearse.api.domain.analysis.entity.ConvertStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateConvertStatusRequest {

    @NotNull(message = "변환 상태는 필수입니다.")
    private ConvertStatus status;

    private String streamingS3Key;
    private String failureReason;
}
