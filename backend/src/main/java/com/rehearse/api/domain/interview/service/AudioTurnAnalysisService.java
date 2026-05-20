package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.models.service.AudioTurnAnalyzer;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedTurnAnalysis;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.AudioChatFallbackRequiredException;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioTurnAnalysisService {

    private static final long MAX_AUDIO_BYTES = 10L * 1024 * 1024;

    private final AudioTurnAnalyzer audioTurnAnalyzer;
    private final TextFallbackTurnAnalyzer textFallbackTurnAnalyzer;
    private final AiCallMetrics aiCallMetrics;

    public AnswerAnalysis analyze(
            Long interviewId,
            MultipartFile audioFile,
            String mainQuestion,
            ReferenceType questionReferenceType
    ) {
        validate(interviewId, audioFile);
        try {
            GeneratedTurnAnalysis raw = audioTurnAnalyzer.analyze(audioFile, mainQuestion, questionReferenceType);
            return raw.toDomain();
        } catch (AudioChatFallbackRequiredException e) {
            log.warn("[AudioTurnAnalysisService] audio chat 실패 → text-only fallback. interviewId={}", interviewId);
            aiCallMetrics.incrementFollowUpSkip("audio_chat_fallback_to_stt");
            return textFallbackTurnAnalyzer.analyze(interviewId, audioFile, mainQuestion, questionReferenceType);
        }
    }

    private static void validate(Long interviewId, MultipartFile audioFile) {
        if (interviewId == null) {
            throw new IllegalArgumentException("interviewId 는 null 일 수 없습니다.");
        }
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(InterviewErrorCode.ANSWER_TEXT_REQUIRED);
        }
        if (audioFile.getSize() > MAX_AUDIO_BYTES) {
            log.warn("[AudioTurnAnalysisService] audio 파일 크기 초과: size={} bytes, max={}",
                    audioFile.getSize(), MAX_AUDIO_BYTES);
            throw new BusinessException(AiErrorCode.CLIENT_ERROR);
        }
    }
}
