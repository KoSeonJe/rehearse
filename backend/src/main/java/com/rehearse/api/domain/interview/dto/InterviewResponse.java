package com.rehearse.api.domain.interview.dto;

import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.domain.questionset.dto.QuestionSetResponse;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class InterviewResponse {

    private final Long id;
    private final Position position;
    private final String positionDetail;
    private final InterviewLevel level;
    private final List<InterviewType> interviewTypes;
    private final List<String> csSubTopics;
    private final InterviewStatus status;
    private final QuestionGenerationStatus questionGenerationStatus;
    private final String failureReason;
    private final Integer durationMinutes;
    private final List<QuestionSetResponse> questionSets;
    private final LocalDateTime createdAt;

    public static InterviewResponse from(Interview interview) {
        return from(interview, Collections.emptyList());
    }

    public static InterviewResponse from(Interview interview, List<QuestionSet> questionSets) {
        List<QuestionSetResponse> questionSetResponses = questionSets.stream()
                .map(QuestionSetResponse::from)
                .toList();

        return InterviewResponse.builder()
                .id(interview.getId())
                .position(interview.getPosition())
                .positionDetail(interview.getPositionDetail())
                .level(interview.getLevel())
                .interviewTypes(new ArrayList<>(interview.getInterviewTypes()))
                .csSubTopics(new ArrayList<>(interview.getCsSubTopics()))
                .status(interview.getStatus())
                .questionGenerationStatus(interview.getQuestionGenerationStatus())
                .failureReason(interview.getFailureReason())
                .durationMinutes(interview.getDurationMinutes())
                .questionSets(questionSetResponses)
                .createdAt(interview.getCreatedAt())
                .build();
    }
}
