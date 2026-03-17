package com.rehearse.api.domain.questionset.dto;

import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.QuestionCategory;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
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

        return QuestionSetResponse.builder()
                .id(questionSet.getId())
                .category(questionSet.getCategory())
                .orderIndex(questionSet.getOrderIndex())
                .analysisStatus(questionSet.getAnalysisStatus())
                .failureReason(questionSet.getFailureReason())
                .questions(questionDetails)
                .build();
    }
}
