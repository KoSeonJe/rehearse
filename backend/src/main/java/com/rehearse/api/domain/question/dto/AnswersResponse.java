package com.rehearse.api.domain.question.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AnswersResponse {

    private final String analysisStatus;
    private final String position;
    private final String techStack;
    private final String level;
    private final List<AnswerResponse> answers;
}
