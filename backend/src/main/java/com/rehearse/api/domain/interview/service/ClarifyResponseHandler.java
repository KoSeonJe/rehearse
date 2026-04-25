package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import com.rehearse.api.infra.ai.prompt.ClarifyResponsePromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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
    private final ClarifyResponsePromptBuilder promptBuilder;

    @Override
    public IntentType supports() {
        return IntentType.CLARIFY_REQUEST;
    }

    @Override
    public FollowUpResponse handle(IntentBranchInput input) {
        try {
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(List.of(
                            ChatMessage.ofCached(ChatMessage.Role.SYSTEM, promptBuilder.buildSystemPrompt()),
                            ChatMessage.of(ChatMessage.Role.USER, promptBuilder.buildUserPrompt(input.context(), input.mainQuestion(), input.answerText()))
                    ))
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

    record ClarifyAiResponse(String question, String ttsQuestion, String reason) {}
}
