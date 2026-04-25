package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
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
public class ClarifyResponseHandler {

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final ClarifyResponsePromptBuilder promptBuilder;

    public FollowUpResponse handle(FollowUpContext context, String mainQuestion, String answerText) {
        try {
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(List.of(
                            ChatMessage.ofCached(ChatMessage.Role.SYSTEM, promptBuilder.buildSystemPrompt()),
                            ChatMessage.of(ChatMessage.Role.USER, promptBuilder.buildUserPrompt(context, mainQuestion, answerText))
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

            return FollowUpResponse.builder()
                    .question(parsed.question())
                    .ttsQuestion(parsed.ttsQuestion())
                    .reason(parsed.reason())
                    .type("CLARIFY_REESTABLISH")
                    .answerText(answerText)
                    .skip(true)
                    .skipReason("CLARIFY_REQUEST")
                    .presentToUser(true)
                    .build();
        } catch (Exception e) {
            log.warn("CLARIFY 응답 생성 실패, 안내 fallback: {}", e.getMessage(), e);
            String safe = "질문이 잘 전달되지 않았을 수 있습니다. 다시 설명드리겠습니다. " + mainQuestion;
            return FollowUpResponse.builder()
                    .question(safe)
                    .ttsQuestion(safe)
                    .type("CLARIFY_FALLBACK")
                    .reason("clarify ai failure fallback")
                    .answerText(answerText)
                    .skip(true)
                    .skipReason("CLARIFY_REQUEST")
                    .presentToUser(true)
                    .build();
        }
    }

    record ClarifyAiResponse(String question, String ttsQuestion, String reason) {}
}
