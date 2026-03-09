package com.devlens.api.domain.interview.dto;

import com.devlens.api.domain.interview.entity.Interview;
import com.devlens.api.domain.interview.entity.InterviewLevel;
import com.devlens.api.domain.interview.entity.InterviewStatus;
import com.devlens.api.domain.interview.entity.InterviewType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class InterviewResponse {

    private final Long id;
    private final String position;
    private final InterviewLevel level;
    private final InterviewType interviewType;
    private final InterviewStatus status;
    private final List<QuestionResponse> questions;
    private final LocalDateTime createdAt;

    public static InterviewResponse from(Interview interview) {
        List<QuestionResponse> questionResponses = interview.getQuestions().stream()
                .map(QuestionResponse::from)
                .toList();

        return InterviewResponse.builder()
                .id(interview.getId())
                .position(interview.getPosition())
                .level(interview.getLevel())
                .interviewType(interview.getInterviewType())
                .status(interview.getStatus())
                .questions(questionResponses)
                .createdAt(interview.getCreatedAt())
                .build();
    }
}
