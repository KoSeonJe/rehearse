package com.rehearse.api.domain.question.models.service;

import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;

import java.util.List;

public interface StandardQuestionGenerator {

    List<GeneratedQuestion> generate(QuestionGenerationRequest request);
}
