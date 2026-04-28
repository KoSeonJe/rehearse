package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.interview.entity.IntentType;
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
public class GiveUpResponseHandler implements IntentResponseHandler {

    static final String SKIP_REASON = "GIVE_UP";
    static final String FALLBACK_TYPE = "GIVE_UP_FALLBACK";
    private static final String FALLBACK_PREFIX = "잠시 다음 내용을 함께 살펴보겠습니다. ";

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final InterviewContextBuilder contextBuilder;
    private final InterviewRuntimeStateCache runtimeStateStore;

    @Override
    public IntentType supports() {
        return IntentType.GIVE_UP;
    }

    @Override
    public FollowUpResponse handle(IntentBranchInput input) {
        try {
            Map<String, Object> runtimeStateMap = resolveRuntimeStateMap(input.interviewId());

            BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                    "giveup_response",
                    runtimeStateMap,
                    input.previousExchanges() != null ? input.previousExchanges() : List.of(),
                    Map.of(
                            "mainQuestion", input.mainQuestion() != null ? input.mainQuestion() : "",
                            "userUtterance", input.answerText() != null ? input.answerText() : "",
                            "personaDepthHint", resolvePersonaHint(input)
                    ),
                    null
            ));

            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(built.messages())
                    .callType("giveup_response")
                    .temperature(0.4)
                    .maxTokens(500)
                    .responseFormat(ResponseFormat.JSON_OBJECT)
                    .build();

            ChatResponse response = aiClient.chat(chatRequest);
            GiveUpAiResponse parsed = aiResponseParser.parseOrRetry(
                    response, GiveUpAiResponse.class, aiClient, chatRequest);

            log.info("GIVE_UP 처리 완료: type={}, reason={}", parsed.type(), parsed.reason());

            return FollowUpResponse.intentBranch(new FollowUpResponse.IntentBranchPayload(
                    parsed.question(), parsed.ttsQuestion(), parsed.reason(),
                    parsed.type(), SKIP_REASON, input.answerText()));
        } catch (RuntimeException e) {
            log.warn("GIVE_UP 응답 생성 실패, 안내 fallback: {}", e.getMessage(), e);
            String safe = FALLBACK_PREFIX + input.mainQuestion();
            return FollowUpResponse.intentBranch(new FollowUpResponse.IntentBranchPayload(
                    safe, safe, "giveup ai failure fallback",
                    FALLBACK_TYPE, SKIP_REASON, input.answerText()));
        }
    }

    private Map<String, Object> resolveRuntimeStateMap(Long interviewId) {
        if (interviewId == null) {
            return Map.of();
        }
        try {
            InterviewRuntimeState state = runtimeStateStore.get(interviewId);
            return Map.of("interviewRuntimeState", state, "interviewId", interviewId);
        } catch (IllegalStateException e) {
            return Map.of();
        }
    }

    private static String resolvePersonaHint(IntentBranchInput input) {
        if (input.context() == null) {
            return "(없음)";
        }
        return input.context().level().name() + " | " + input.context().position().name();
    }

    record GiveUpAiResponse(String question, String ttsQuestion, String reason, String type) {}
}
