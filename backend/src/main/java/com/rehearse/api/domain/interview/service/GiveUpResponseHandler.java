package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import com.rehearse.api.infra.ai.prompt.GiveUpResponsePromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GiveUpResponseHandler implements IntentResponseHandler {

    static final String SKIP_REASON = "GIVE_UP";
    static final String FALLBACK_TYPE = "GIVE_UP_FALLBACK";
    private static final String FALLBACK_PREFIX = "잠시 다음 내용을 함께 살펴보겠습니다. ";

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final GiveUpResponsePromptBuilder promptBuilder;

    @Override
    public IntentType supports() {
        return IntentType.GIVE_UP;
    }

    @Override
    public FollowUpResponse handle(IntentBranchInput input) {
        try {
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(List.of(
                            ChatMessage.ofCached(ChatMessage.Role.SYSTEM, promptBuilder.buildSystemPrompt()),
                            ChatMessage.of(ChatMessage.Role.USER, promptBuilder.buildUserPrompt(input.context(), input.mainQuestion(), input.answerText()))
                    ))
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

    record GiveUpAiResponse(String question, String ttsQuestion, String reason, String type) {}
}
