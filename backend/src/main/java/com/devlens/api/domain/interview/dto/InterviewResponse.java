package com.devlens.api.domain.interview.dto;

import com.devlens.api.domain.interview.entity.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class InterviewResponse {

    private final Long id;
    private final Position position;
    private final String positionDetail;
    private final InterviewLevel level;
    private final List<InterviewType> interviewTypes;
    private final InterviewStatus status;
    private final Integer durationMinutes;
    private final List<QuestionResponse> questions;
    private final LocalDateTime createdAt;

    public static InterviewResponse from(Interview interview) {
        List<QuestionResponse> questionResponses = interview.getQuestions().stream()
                .map(QuestionResponse::from)
                .toList();

        return InterviewResponse.builder()
                .id(interview.getId())
                .position(interview.getPosition())
                .positionDetail(interview.getPositionDetail())
                .level(interview.getLevel())
                .interviewTypes(interview.getInterviewTypeList())
                .status(interview.getStatus())
                .durationMinutes(interview.getDurationMinutes())
                .questions(questionResponses)
                .createdAt(interview.getCreatedAt())
                .build();
    }
}
