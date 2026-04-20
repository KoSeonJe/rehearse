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

    /**
     * 범용 채팅 호출.
     * 후속 plan 들이 신규 AI 호출 추가 시 이 메서드를 사용한다.
     * 기존 3개 도메인 메서드는 내부에서 이 메서드 경유로 위임한다.
     */
    ChatResponse chat(ChatRequest request);

    List<GeneratedQuestion> generateQuestions(QuestionGenerationRequest request);

    GeneratedFollowUp generateFollowUpQuestion(FollowUpGenerationRequest request);

    GeneratedFollowUp generateFollowUpWithAudio(MultipartFile audioFile, FollowUpGenerationRequest request);
}
