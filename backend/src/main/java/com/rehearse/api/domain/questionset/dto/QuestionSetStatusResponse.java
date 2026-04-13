package com.rehearse.api.domain.questionset.dto;

import com.rehearse.api.domain.file.entity.FileStatus;
import com.rehearse.api.domain.analysis.entity.AnalysisStatus;
import com.rehearse.api.domain.analysis.entity.ConvertStatus;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.analysis.entity.QuestionSetAnalysis;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionSetStatusResponse {

    private final Long id;
    private final AnalysisStatus analysisStatus;
    private final ConvertStatus convertStatus;
    private final FileStatus fileStatus;
    private final boolean isVerbalCompleted;
    private final boolean isNonverbalCompleted;
    private final boolean fullyReady;
    private final String failureReason;

    public static QuestionSetStatusResponse from(QuestionSet questionSet) {
        FileStatus fileStatus = questionSet.getFileMetadata() != null
                ? questionSet.getFileMetadata().getStatus()
                : null;

        QuestionSetAnalysis analysis = questionSet.getAnalysis();

        return QuestionSetStatusResponse.builder()
                .id(questionSet.getId())
                .analysisStatus(questionSet.getEffectiveAnalysisStatus())
                .convertStatus(analysis != null ? analysis.getConvertStatus() : ConvertStatus.PENDING)
                .fileStatus(fileStatus)
                .isVerbalCompleted(analysis != null && analysis.isVerbalCompleted())
                .isNonverbalCompleted(analysis != null && analysis.isNonverbalCompleted())
                .fullyReady(analysis != null && analysis.isFullyReady())
                .failureReason(questionSet.getAnalysisFailureReason())
                .build();
    }
}
