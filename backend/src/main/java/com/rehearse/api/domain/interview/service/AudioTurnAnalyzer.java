package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.GeneratedTurnAnalysis;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.AudioChatFallbackRequiredException;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import com.rehearse.api.infra.ai.prompt.AudioTurnAnalyzerPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioTurnAnalyzer {

    private static final String CALL_TYPE = "audio_turn_analyzer";
    private static final double TEMPERATURE = 0.2;
    private static final int MAX_TOKENS = 1024;
    private static final long MAX_AUDIO_BYTES = 10L * 1024 * 1024;

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final AudioTurnAnalyzerPromptBuilder promptBuilder;
    private final TextFallbackTurnAnalyzer textFallbackTurnAnalyzer;
    private final AiCallMetrics aiCallMetrics;

    public AnswerAnalysis analyze(
            Long interviewId,
            MultipartFile audioFile,
            String mainQuestion,
            ReferenceType questionReferenceType,
            boolean isResumeTrack
    ) {
        validate(interviewId, audioFile);
        try {
            return analyzeViaAudioChat(audioFile, mainQuestion, questionReferenceType);
        } catch (AudioChatFallbackRequiredException e) {
            log.warn("[AudioTurnAnalyzer] audio chat 실패 → text-only fallback. interviewId={}", interviewId);
            aiCallMetrics.incrementFollowUpSkip("audio_chat_fallback_to_stt");
            return textFallbackTurnAnalyzer.analyze(interviewId, audioFile, mainQuestion, questionReferenceType, isResumeTrack);
        }
    }

    private AnswerAnalysis analyzeViaAudioChat(
            MultipartFile audio,
            String mainQuestion,
            ReferenceType refType
    ) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPromptText(mainQuestion, refType);

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.ofCached(ChatMessage.Role.SYSTEM, systemPrompt),
                        ChatMessage.of(ChatMessage.Role.USER, userPrompt)
                ))
                .callType(CALL_TYPE)
                .temperature(TEMPERATURE)
                .maxTokens(MAX_TOKENS)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .build();

        ChatResponse response = aiClient.chatWithAudio(chatRequest, audio);
        GeneratedTurnAnalysis raw = aiResponseParser.parseJsonResponse(response.content(), GeneratedTurnAnalysis.class);
        return raw.toDomain();
    }

    private static void validate(Long interviewId, MultipartFile audioFile) {
        if (interviewId == null) {
            throw new IllegalArgumentException("interviewId 는 null 일 수 없습니다.");
        }
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(InterviewErrorCode.ANSWER_TEXT_REQUIRED);
        }
        if (audioFile.getSize() > MAX_AUDIO_BYTES) {
            log.warn("[AudioTurnAnalyzer] audio 파일 크기 초과: size={} bytes, max={}",
                    audioFile.getSize(), MAX_AUDIO_BYTES);
            throw new BusinessException(AiErrorCode.CLIENT_ERROR);
        }
    }
}
