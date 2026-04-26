package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.Perspective;
import com.rehearse.api.domain.interview.RecommendedNextAction;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.repository.InterviewRuntimeStateStore;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerAnalyzer {

    private static final String CALL_TYPE = "answer_analyzer";
    private static final double TEMPERATURE = 0.2;
    private static final int MAX_TOKENS = 800;

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final InterviewContextBuilder contextBuilder;
    private final InterviewRuntimeStateStore runtimeStateStore;

    public AnswerAnalysis analyze(
            Long interviewId,
            Long turnId,
            String mainQuestion,
            ReferenceType questionReferenceType,
            String userAnswer,
            List<Perspective> askedPerspectives
    ) {
        if (interviewId == null || turnId == null) {
            throw new IllegalArgumentException("interviewId/turnId 는 null 일 수 없습니다.");
        }

        InterviewRuntimeState runtimeState = runtimeStateStore.get(interviewId);
        String personaDepthHint = toReferenceLabel(questionReferenceType);
        String askedPerspectivesStr = formatPerspectives(askedPerspectives);

        BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                CALL_TYPE,
                Map.of("interviewRuntimeState", runtimeState, "interviewId", interviewId),
                List.of(),
                Map.of(
                        "mainQuestion", mainQuestion != null ? mainQuestion : "",
                        "userAnswer", userAnswer != null ? userAnswer : "",
                        "personaDepthHint", personaDepthHint + " | ASKED_PERSPECTIVES: " + askedPerspectivesStr
                ),
                null
        ));

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(built.messages())
                .callType(CALL_TYPE)
                .temperature(TEMPERATURE)
                .maxTokens(MAX_TOKENS)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .build();

        ChatResponse response = aiClient.chat(chatRequest);
        AnswerAnalysis parsed = aiResponseParser.parseOrRetry(
                response, AnswerAnalysis.class, aiClient, chatRequest);

        AnswerAnalysis withTurnId = parsed.withTurnId(turnId);
        AnswerAnalysis guarded = applyL1FalseNegativeGuard(withTurnId);

        runtimeStateStore.update(interviewId, state -> state.recordAnalysis(turnId, guarded));

        if (guarded.recommendedNextAction() != withTurnId.recommendedNextAction()) {
            log.info("[AnswerAnalyzer] L1 FN 가드 적용: interviewId={}, turnId={}, override→CLARIFICATION",
                    interviewId, turnId);
        }

        return guarded;
    }

    private AnswerAnalysis applyL1FalseNegativeGuard(AnswerAnalysis analysis) {
        boolean noClaims = analysis.claims().isEmpty();
        boolean lowQuality = analysis.answerQuality() <= 1;
        if (noClaims && lowQuality && analysis.recommendedNextAction() != RecommendedNextAction.CLARIFICATION) {
            return analysis.withRecommendedNextAction(RecommendedNextAction.CLARIFICATION);
        }
        return analysis;
    }

    private static String toReferenceLabel(ReferenceType refType) {
        if (refType == null) {
            return "CONCEPT";
        }
        return switch (refType) {
            case GUIDE -> "EXPERIENCE";
            case MODEL_ANSWER -> "CONCEPT";
        };
    }

    private static String formatPerspectives(List<Perspective> askedPerspectives) {
        if (askedPerspectives == null || askedPerspectives.isEmpty()) {
            return "(없음)";
        }
        return askedPerspectives.stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
