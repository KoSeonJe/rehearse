package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
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
public class GiveUpResponseHandler {

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final GiveUpResponsePromptBuilder promptBuilder;

    public FollowUpResponse handle(FollowUpContext context, String mainQuestion, String answerText) {
        try {
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(List.of(
                            ChatMessage.ofCached(ChatMessage.Role.SYSTEM, promptBuilder.buildSystemPrompt()),
                            ChatMessage.of(ChatMessage.Role.USER, promptBuilder.buildUserPrompt(context, mainQuestion, answerText))
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

            return FollowUpResponse.builder()
                    .question(parsed.question())
                    .ttsQuestion(parsed.ttsQuestion())
                    .reason(parsed.reason())
                    .type(parsed.type())
                    .answerText(answerText)
                    .skip(true)
                    .skipReason("GIVE_UP")
                    .presentToUser(true)
                    .build();
        } catch (Exception e) {
            log.warn("GIVE_UP 응답 생성 실패, 안내 fallback: {}", e.getMessage(), e);
            String safe = "잠시 다음 내용을 함께 살펴보겠습니다. " + mainQuestion;
            return FollowUpResponse.builder()
                    .question(safe)
                    .ttsQuestion(safe)
                    .type("GIVE_UP_FALLBACK")
                    .reason("giveup ai failure fallback")
                    .answerText(answerText)
                    .skip(true)
                    .skipReason("GIVE_UP")
                    .presentToUser(true)
                    .build();
        }
    }

    record GiveUpAiResponse(String question, String ttsQuestion, String reason, String type) {}
}
