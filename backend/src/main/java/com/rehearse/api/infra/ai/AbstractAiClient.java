package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.adapter.QuestionGenerationAdapter;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public abstract class AbstractAiClient implements AiClient {

    protected final QuestionGenerationAdapter questionAdapter;

    protected AbstractAiClient(QuestionGenerationAdapter questionAdapter) {
        this.questionAdapter = questionAdapter;
    }

    @Override
    public List<GeneratedQuestion> generateQuestions(QuestionGenerationRequest request) {
        return questionAdapter.adapt(this, request);
    }

    // 기본 구현은 미지원 — audio chat 을 지원하는 client 만 override.
    @Override
    public ChatResponse chatWithAudio(ChatRequest request, MultipartFile audio) {
        throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
    }
}
