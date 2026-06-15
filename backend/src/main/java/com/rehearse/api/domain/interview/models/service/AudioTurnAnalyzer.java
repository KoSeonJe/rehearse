package com.rehearse.api.domain.interview.models.service;

import com.rehearse.api.domain.question.entity.QuestionCategory;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.dto.GeneratedTurnAnalysis;
import com.rehearse.api.infra.ai.exception.AudioChatFallbackRequiredException;
import org.springframework.web.multipart.MultipartFile;

/**
 * 음성 답변을 LLM audio chat 으로 전사+분석하는 port.
 *
 * <p>구현체는 OpenAI 어댑터 + Mock fallback. Claude/Resilient 미지원 (audio chat = OpenAI 단독).
 * 인프라 오류 / 네트워크 오류 / 비결함 BusinessException 은 {@link AudioChatFallbackRequiredException}
 * 으로 변환되어 caller 가 STT + text-only 경로로 우회하도록 한다.
 */
public interface AudioTurnAnalyzer {

    GeneratedTurnAnalysis analyze(
            MultipartFile audio,
            String mainQuestion,
            ReferenceType questionReferenceType,
            QuestionCategory category
    ) throws AudioChatFallbackRequiredException;
}
