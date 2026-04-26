package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.repository.InterviewRuntimeStateStore;
import com.rehearse.api.domain.interview.vo.IntentType;
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
public class ClarifyResponseHandler implements IntentResponseHandler {

    static final String SKIP_REASON = "CLARIFY_REQUEST";
    static final String TYPE = "CLARIFY_REESTABLISH";
    static final String FALLBACK_TYPE = "CLARIFY_FALLBACK";
    private static final String FALLBACK_PREFIX = "질문이 잘 전달되지 않았을 수 있습니다. 다시 설명드리겠습니다. ";

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final InterviewContextBuilder contextBuilder;
    private final InterviewRuntimeStateStore runtimeStateStore;

    @Override
    public IntentType supports() {
        return IntentType.CLARIFY_REQUEST;
    }

    @Override
    public FollowUpResponse handle(IntentBranchInput input) {
        try {
            Map<String, Object> runtimeStateMap = resolveRuntimeStateMap(input.interviewId());

            BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                    "clarify_response",
                    runtimeStateMap,
                    input.previousExchanges() != null ? input.previousExchanges() : List.of(),
                    Map.of(
                            "mainQuestion", input.mainQuestion() != null ? input.mainQuestion() : "",
                            "userUtterance", input.answerText() != null ? input.answerText() : ""
                    ),
                    null
            ));

            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(built.messages())
                    .callType("clarify_response")
                    .temperature(0.4)
                    .maxTokens(400)
                    .responseFormat(ResponseFormat.JSON_OBJECT)
                    .build();

            ChatResponse response = aiClient.chat(chatRequest);
            ClarifyAiResponse parsed = aiResponseParser.parseOrRetry(
                    response, ClarifyAiResponse.class, aiClient, chatRequest);

            log.info("CLARIFY_REQUEST 처리 완료: reason={}", parsed.reason());

            return FollowUpResponse.intentBranch(new FollowUpResponse.IntentBranchPayload(
                    parsed.question(), parsed.ttsQuestion(), parsed.reason(),
                    TYPE, SKIP_REASON, input.answerText()));
        } catch (RuntimeException e) {
            log.warn("CLARIFY 응답 생성 실패, 안내 fallback: {}", e.getMessage(), e);
            String safe = FALLBACK_PREFIX + input.mainQuestion();
            return FollowUpResponse.intentBranch(new FollowUpResponse.IntentBranchPayload(
                    safe, safe, "clarify ai failure fallback",
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

    record ClarifyAiResponse(String question, String ttsQuestion, String reason) {}
}
