package com.rehearse.api.infra.ai;

import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;

import java.util.List;

public interface AiClient {

    List<GeneratedQuestion> generateQuestions(QuestionGenerationRequest request);

    GeneratedFollowUp generateFollowUpQuestion(FollowUpGenerationRequest request);
}
