package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.interview.entity.AskedPerspectives;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
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
    private final InterviewRuntimeStateCache runtimeStateStore;
    private final TextFallbackTurnAnalyzer textFallbackTurnAnalyzer;
    private final AiCallMetrics aiCallMetrics;

    public TurnAnalysisResult analyze(
            Long interviewId,
            Long turnId,
            MultipartFile audioFile,
            String mainQuestion,
            ReferenceType questionReferenceType,
            AskedPerspectives askedPerspectives
    ) {
        validate(interviewId, turnId, audioFile);
        try {
            TurnAnalysisResult viaAudio = analyzeViaAudioChat(audioFile, mainQuestion, questionReferenceType, askedPerspectives);
            return commit(interviewId, turnId, viaAudio);
        } catch (AudioChatFallbackRequiredException e) {
            log.warn("[AudioTurnAnalyzer] audio chat 실패 → text-only fallback. interviewId={}", interviewId);
            aiCallMetrics.incrementFollowUpSkip("audio_chat_fallback_to_stt");
            // fallback 경로: TextFallback 의 AnswerAnalyzer 가 내부적으로 가드+캐시 처리하므로 commit() bypass.
            return textFallbackTurnAnalyzer.analyze(
                    interviewId, turnId, audioFile, mainQuestion, questionReferenceType, askedPerspectives);
        }
    }

    private TurnAnalysisResult analyzeViaAudioChat(
            MultipartFile audio,
            String mainQuestion,
            ReferenceType refType,
            AskedPerspectives askedPerspectives
    ) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPromptText(mainQuestion, refType, askedPerspectives.values());

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
        // parseOrRetry 는 retry 시 text-only chat 호출 → audio 컨텍스트 손실. audio 경로는 단발 파싱만.
        return aiResponseParser.parseJsonResponse(response.content(), TurnAnalysisResult.class);
    }

    private TurnAnalysisResult commit(Long interviewId, Long turnId, TurnAnalysisResult viaAudio) {
        AnswerAnalysis withTurnId = viaAudio.answerAnalysis() != null
                ? viaAudio.answerAnalysis().withTurnId(turnId)
                : AnswerAnalysis.empty(turnId);
        AnswerAnalysis guarded = withTurnId.applyL1FalseNegativeGuard(viaAudio.intent().type());
        runtimeStateStore.update(interviewId, state -> state.recordAnalysis(turnId, guarded));
        if (guarded.recommendedNextAction() != withTurnId.recommendedNextAction()) {
            log.info("[AudioTurnAnalyzer] L1 FN 가드 적용: interviewId={}, turnId={}, override→CLARIFICATION",
                    interviewId, turnId);
        }
        return viaAudio.withAnswerAnalysis(guarded);
    }

    private static void validate(Long interviewId, Long turnId, MultipartFile audioFile) {
        if (interviewId == null || turnId == null) {
            throw new IllegalArgumentException("interviewId/turnId 는 null 일 수 없습니다.");
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
