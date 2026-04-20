package com.rehearse.api.infra.ai;

import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AiClient {

    ChatResponse chat(ChatRequest request);

    List<GeneratedQuestion> generateQuestions(QuestionGenerationRequest request);

    GeneratedFollowUp generateFollowUpQuestion(FollowUpGenerationRequest request);

    GeneratedFollowUp generateFollowUpWithAudio(MultipartFile audioFile, FollowUpGenerationRequest request);
}
