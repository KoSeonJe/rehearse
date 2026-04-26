package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.adapter.FollowUpGenerationAdapter;
import com.rehearse.api.infra.ai.adapter.QuestionGenerationAdapter;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public abstract class AbstractAiClient implements AiClient {

    protected final QuestionGenerationAdapter questionAdapter;
    protected final FollowUpGenerationAdapter followUpAdapter;
    protected final SttService sttService;

    protected AbstractAiClient(
            QuestionGenerationAdapter questionAdapter,
            FollowUpGenerationAdapter followUpAdapter,
            SttService sttService) {
        this.questionAdapter = questionAdapter;
        this.followUpAdapter = followUpAdapter;
        this.sttService = sttService;
    }

    @Override
    public List<GeneratedQuestion> generateQuestions(QuestionGenerationRequest request) {
        return questionAdapter.adapt(this, request);
    }

    @Override
    public GeneratedFollowUp generateFollowUpQuestion(FollowUpGenerationRequest request) {
        return followUpAdapter.adapt(this, request);
    }

    @Override
    public GeneratedFollowUp generateFollowUpWithAudio(MultipartFile audioFile, FollowUpGenerationRequest request) {
        return followUpAdapter.adaptWithAudio(this, audioFile, request, sttService);
    }

    // 기본 구현은 미지원 — audio chat 을 지원하는 client 만 override.
    @Override
    public ChatResponse chatWithAudio(ChatRequest request, MultipartFile audio) {
        throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
    }
}
