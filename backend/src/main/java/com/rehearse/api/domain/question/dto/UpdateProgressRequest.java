package com.rehearse.api.domain.question.dto;

import com.rehearse.api.domain.question.entity.AnalysisStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProgressRequest {

    @NotNull(message = "분석 상태는 필수입니다.")
    private AnalysisStatus status;

    private String failureReason;
    private String failureDetail;
}
