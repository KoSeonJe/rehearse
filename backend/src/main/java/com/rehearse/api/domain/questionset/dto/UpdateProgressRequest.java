package com.rehearse.api.domain.questionset.dto;

import com.rehearse.api.domain.questionset.entity.AnalysisProgress;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProgressRequest {

    @NotNull(message = "분석 진행 상태는 필수입니다.")
    private AnalysisProgress progress;

    private String failureReason;
    private String failureDetail;
}
