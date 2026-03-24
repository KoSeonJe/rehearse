package com.rehearse.api.domain.questionset.dto;

import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.QuestionCategory;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetAnalysis;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuestionSetResponse {

    private final Long id;
    private final QuestionCategory category;
    private final int orderIndex;
    private final AnalysisStatus analysisStatus;
    private final String failureReason;
    private final List<QuestionDetailResponse> questions;

    public static QuestionSetResponse from(QuestionSet questionSet) {
        List<QuestionDetailResponse> questionDetails = questionSet.getQuestions().stream()
                .map(QuestionDetailResponse::from)
                .toList();

        QuestionSetAnalysis analysis = questionSet.getAnalysis();
        AnalysisStatus status = analysis != null ? analysis.getAnalysisStatus() : AnalysisStatus.PENDING;
        String failureReason = analysis != null ? analysis.getFailureReason() : null;

        return QuestionSetResponse.builder()
                .id(questionSet.getId())
                .category(questionSet.getCategory())
                .orderIndex(questionSet.getOrderIndex())
                .analysisStatus(status)
                .failureReason(failureReason)
                .questions(questionDetails)
                .build();
    }
}
