package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.interview.models.service.AudioTurnAnalyzer;
import com.rehearse.api.domain.question.entity.QuestionCategory;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.adapter.OpenAiAudioTurnAnalyzer;
import com.rehearse.api.infra.ai.dto.GeneratedTurnAnalysis;
import com.rehearse.api.infra.ai.exception.AudioChatFallbackRequiredException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@ConditionalOnMissingBean(OpenAiAudioTurnAnalyzer.class)
public class MockAudioTurnAnalyzer implements AudioTurnAnalyzer {

    @PostConstruct
    void init() {
        log.warn("=== MockAudioTurnAnalyzer 활성화: API 키 없이 항상 text-only fallback 으로 우회합니다 ===");
    }

    // audio chat 미지원 환경에서 caller(AudioTurnAnalysisService) 가 STT + text-only 경로로 자동 진입하도록 시그널 throw.
    @Override
    public GeneratedTurnAnalysis analyze(
            MultipartFile audio,
            String mainQuestion,
            ReferenceType questionReferenceType,
            QuestionCategory category
    ) throws AudioChatFallbackRequiredException {
        throw new AudioChatFallbackRequiredException("Mock 환경 — audio chat 미지원");
    }
}
