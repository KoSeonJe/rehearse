package com.rehearse.api.domain.questionset.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AnswersResponse {

    private final String analysisStatus;
    private final List<AnswerResponse> answers;
}
