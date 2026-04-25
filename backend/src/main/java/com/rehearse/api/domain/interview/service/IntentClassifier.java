package com.rehearse.api.domain.interview.service;

import com.rehearse.api.global.config.IntentClassifierProperties;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.vo.IntentResult;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import com.rehearse.api.infra.ai.prompt.IntentClassifierPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentClassifier {

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final IntentClassifierPromptBuilder promptBuilder;
    private final IntentClassifierProperties properties;

    public IntentResult classify(String mainQuestion, String answerText, List<FollowUpExchange> previousExchanges) {
        try {
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(List.of(
                            ChatMessage.ofCached(ChatMessage.Role.SYSTEM, promptBuilder.buildSystemPrompt()),
                            ChatMessage.of(ChatMessage.Role.USER, promptBuilder.buildUserPrompt(mainQuestion, answerText, previousExchanges))
                    ))
                    .callType("intent_classifier")
                    .temperature(0.1)
                    .maxTokens(200)
                    .responseFormat(ResponseFormat.JSON_OBJECT)
                    .build();

            ChatResponse response = aiClient.chat(chatRequest);
            IntentClassificationResponse parsed = aiResponseParser.parseOrRetry(
                    response, IntentClassificationResponse.class, aiClient, chatRequest);

            IntentType intentType = parseIntent(parsed.intent());
            if (intentType == null) {
                log.info("Intent 파싱 불가 (raw='{}') → forceAnswer fallback", parsed.intent());
                return IntentResult.forceAnswer();
            }

            if (parsed.confidence() < properties.fallbackOnLowConf()) {
                log.info("Intent 분류 신뢰도 낮음 ({}) → forceAnswer fallback", parsed.confidence());
                return IntentResult.forceAnswer();
            }

            return IntentResult.of(intentType, parsed.confidence(), parsed.reasoning());

        } catch (RuntimeException e) {
            log.warn("Intent 분류 실패, forceAnswer fallback 적용: {}", e.getMessage(), e);
            return IntentResult.forceAnswer();
        }
    }

    private IntentType parseIntent(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toUpperCase().replace('-', '_');
        try {
            return IntentType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    record IntentClassificationResponse(String intent, double confidence, String reasoning) {}
}
