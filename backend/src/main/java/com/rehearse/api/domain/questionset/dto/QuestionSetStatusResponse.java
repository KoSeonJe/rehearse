package com.rehearse.api.domain.questionset.dto;

import com.rehearse.api.domain.file.entity.FileStatus;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.ConvertStatus;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetAnalysis;
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
        if (analysis == null) {
            return QuestionSetStatusResponse.builder()
                    .id(questionSet.getId())
                    .analysisStatus(AnalysisStatus.PENDING)
                    .convertStatus(ConvertStatus.PENDING)
                    .fileStatus(fileStatus)
                    .build();
        }

        return QuestionSetStatusResponse.builder()
                .id(questionSet.getId())
                .analysisStatus(analysis.getAnalysisStatus())
                .convertStatus(analysis.getConvertStatus())
                .fileStatus(fileStatus)
                .isVerbalCompleted(analysis.isVerbalCompleted())
                .isNonverbalCompleted(analysis.isNonverbalCompleted())
                .fullyReady(analysis.isFullyReady())
                .failureReason(analysis.getFailureReason())
                .build();
    }
}
