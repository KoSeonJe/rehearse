package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.Perspective;
import com.rehearse.api.domain.interview.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.TurnAnalysisResult;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.repository.InterviewRuntimeStateStore;
import com.rehearse.api.domain.interview.vo.IntentResult;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.SttService;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.prompt.AudioTurnAnalyzerPromptBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
public class AudioTurnAnalyzer {

    private static final String CALL_TYPE = "audio_turn_analyzer";
    private static final double TEMPERATURE = 0.2;
    private static final int MAX_TOKENS = 1024;
    private static final long MAX_AUDIO_BYTES = 10L * 1024 * 1024;

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final AudioTurnAnalyzerPromptBuilder promptBuilder;
    private final InterviewRuntimeStateStore runtimeStateStore;
    private final SttService sttService;
    private final IntentClassifier intentClassifier;
    private final AnswerAnalyzer answerAnalyzer;
    private final com.rehearse.api.infra.ai.metrics.AiCallMetrics aiCallMetrics;

    public AudioTurnAnalyzer(
            AiClient aiClient,
            AiResponseParser aiResponseParser,
            AudioTurnAnalyzerPromptBuilder promptBuilder,
            InterviewRuntimeStateStore runtimeStateStore,
            @Nullable SttService sttService,
            IntentClassifier intentClassifier,
            AnswerAnalyzer answerAnalyzer,
            com.rehearse.api.infra.ai.metrics.AiCallMetrics aiCallMetrics
    ) {
        this.aiClient = aiClient;
        this.aiResponseParser = aiResponseParser;
        this.promptBuilder = promptBuilder;
        this.runtimeStateStore = runtimeStateStore;
        this.sttService = sttService;
        this.intentClassifier = intentClassifier;
        this.answerAnalyzer = answerAnalyzer;
        this.aiCallMetrics = aiCallMetrics;
    }

    public TurnAnalysisResult analyze(
            Long interviewId,
            Long turnId,
            MultipartFile audioFile,
            String mainQuestion,
            ReferenceType questionReferenceType,
            List<Perspective> askedPerspectives
    ) {
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

        try {
            TurnAnalysisResult viaAudio = analyzeViaAudioChat(audioFile, mainQuestion, questionReferenceType, askedPerspectives);
            // audio 경로: 가드 + 캐시는 본 클래스가 책임
            AnswerAnalysis withTurnId = viaAudio.answerAnalysis() != null
                    ? viaAudio.answerAnalysis().withTurnId(turnId)
                    : emptyAnalysis(turnId);
            AnswerAnalysis guarded = applyL1FalseNegativeGuard(viaAudio.intent().type(), withTurnId);
            runtimeStateStore.update(interviewId, state -> state.recordAnalysis(turnId, guarded));
            if (guarded.recommendedNextAction() != withTurnId.recommendedNextAction()) {
                log.info("[AudioTurnAnalyzer] L1 FN 가드 적용: interviewId={}, turnId={}, override→CLARIFICATION",
                        interviewId, turnId);
            }
            return viaAudio.withAnswerAnalysis(guarded);
        } catch (BusinessException e) {
            if (!isAudioUnsupported(e)) {
                throw e;
            }
            log.warn("[AudioTurnAnalyzer] audio chat 미지원/실패 → text-only fallback. interviewId={}, code={}",
                    interviewId, e.getCode());
            aiCallMetrics.incrementFollowUpSkip("audio_chat_fallback_to_stt");
            // fallback 경로: AnswerAnalyzer 가 내부적으로 가드 + 캐시 처리하므로 본 클래스 epilogue bypass
            return analyzeViaTextFallback(interviewId, turnId, audioFile, mainQuestion, questionReferenceType, askedPerspectives);
        }
    }

    private TurnAnalysisResult analyzeViaAudioChat(
            MultipartFile audio,
            String mainQuestion,
            ReferenceType refType,
            List<Perspective> askedPerspectives
    ) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPromptText(mainQuestion, refType, askedPerspectives);

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

    private TurnAnalysisResult analyzeViaTextFallback(
            Long interviewId,
            Long turnId,
            MultipartFile audioFile,
            String mainQuestion,
            ReferenceType refType,
            List<Perspective> askedPerspectives
    ) {
        if (sttService == null) {
            log.error("[AudioTurnAnalyzer] STT 서비스 미설정 — fallback 불가. interviewId={}", interviewId);
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
        String answerText = sttService.transcribe(audioFile);
        IntentResult intent = intentClassifier.classify(mainQuestion, answerText, List.of());

        AnswerAnalysis analysis;
        if (intent.type() == IntentType.ANSWER) {
            analysis = answerAnalyzer.analyze(interviewId, turnId, mainQuestion, refType, answerText, askedPerspectives);
        } else {
            analysis = emptyAnalysis(turnId);
        }
        return new TurnAnalysisResult(answerText, intent, analysis);
    }

    private AnswerAnalysis applyL1FalseNegativeGuard(IntentType intentType, AnswerAnalysis analysis) {
        if (intentType != IntentType.ANSWER) {
            return analysis;
        }
        boolean noClaims = analysis.claims().isEmpty();
        boolean lowQuality = analysis.answerQuality() <= 1;
        if (noClaims && lowQuality && analysis.recommendedNextAction() != RecommendedNextAction.CLARIFICATION) {
            return analysis.withRecommendedNextAction(RecommendedNextAction.CLARIFICATION);
        }
        return analysis;
    }

    private static AnswerAnalysis emptyAnalysis(Long turnId) {
        return new AnswerAnalysis(turnId, List.of(), List.of(), List.of(), 1, RecommendedNextAction.CLARIFICATION);
    }

    private static boolean isAudioUnsupported(BusinessException e) {
        String code = e.getCode();
        return AiErrorCode.SERVICE_UNAVAILABLE.getCode().equals(code)
                || AiErrorCode.CLIENT_ERROR.getCode().equals(code);
    }
}
