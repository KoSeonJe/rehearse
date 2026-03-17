package com.rehearse.api.domain.questionset.dto;

import com.rehearse.api.domain.file.entity.FileStatus;
import com.rehearse.api.domain.questionset.entity.AnalysisProgress;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionSetStatusResponse {

    private final Long id;
    private final AnalysisStatus analysisStatus;
    private final AnalysisProgress analysisProgress;
    private final FileStatus fileStatus;
    private final String failureReason;

    public static QuestionSetStatusResponse from(QuestionSet questionSet) {
        FileStatus fileStatus = questionSet.getFileMetadata() != null
                ? questionSet.getFileMetadata().getStatus()
                : null;

        return QuestionSetStatusResponse.builder()
                .id(questionSet.getId())
                .analysisStatus(questionSet.getAnalysisStatus())
                .analysisProgress(questionSet.getAnalysisProgress())
                .fileStatus(fileStatus)
                .failureReason(questionSet.getFailureReason())
                .build();
    }
}
