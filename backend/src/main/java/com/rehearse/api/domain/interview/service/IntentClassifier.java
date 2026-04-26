package com.rehearse.api.domain.interview.service;

import com.rehearse.api.global.config.IntentClassifierProperties;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.vo.IntentResult;
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
public class IntentClassifier {

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final InterviewContextBuilder contextBuilder;
    private final IntentClassifierProperties properties;

    public IntentResult classify(String mainQuestion, String answerText, List<FollowUpExchange> previousExchanges) {
        try {
            BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                    "intent_classifier",
                    Map.of(),
                    previousExchanges != null ? previousExchanges : List.of(),
                    Map.of(
                            "mainQuestion", mainQuestion != null ? mainQuestion : "",
                            "userUtterance", answerText != null ? answerText : ""
                    ),
                    null
            ));

            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(built.messages())
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

            if (parsed.confidence() < properties.fallbackOnLowConfidence()) {
                log.info("Intent 분류 신뢰도 낮음 ({}) → forceAnswer fallback", parsed.confidence());
                return IntentResult.forceAnswer();
            }

            return IntentResult.of(intentType, parsed.confidence(), parsed.reasoning());

        } catch (Exception e) {
            // 어떤 실패든 ANSWER 안전 환원: 분류 실패가 사용자 path 차단 금지
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
