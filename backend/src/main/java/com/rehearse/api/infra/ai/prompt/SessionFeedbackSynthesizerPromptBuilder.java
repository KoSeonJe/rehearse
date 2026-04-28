package com.rehearse.api.infra.ai.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.infra.ai.dto.CachePolicy;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionFeedbackSynthesizerPromptBuilder {

    private static final String CALL_TYPE = "feedback_synthesizer";
    private static final String TEMPLATE_PATH = "classpath:prompts/template/session-feedback-synthesizer.txt";

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Value("${rehearse.feedback-synthesizer.model:gpt-4o-mini}")
    private String model;

    @Value("${rehearse.feedback-synthesizer.temperature:0.4}")
    private double temperature;

    @Value("${rehearse.feedback-synthesizer.max-tokens:2048}")
    private int maxTokens;

    private String template;

    @PostConstruct
    void init() throws IOException {
        var resource = resourceLoader.getResource(TEMPLATE_PATH);
        template = resource.getContentAsString(StandardCharsets.UTF_8);
        log.info("SessionFeedbackSynthesizerPromptBuilder 초기화 완료");
    }

    public ChatRequest build(SessionFeedbackInput input) {
        String userPrompt = buildUserPrompt(input);

        return ChatRequest.builder()
                .messages(List.of(ChatMessage.of(ChatMessage.Role.USER, userPrompt)))
                .modelOverride(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .cachePolicy(CachePolicy.defaults())
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .callType(CALL_TYPE)
                .build();
    }

    private String buildUserPrompt(SessionFeedbackInput input) {
        return template
                .replace("{{SESSION_METADATA}}", serialize(input.sessionMetadata()))
                .replace("{{USER_LEVEL}}", input.userLevel() != null ? input.userLevel().name() : "MID")
                .replace("{{COVERAGE}}", input.coverage() != null ? input.coverage() : "all turns scored")
                .replace("{{TURN_SCORES_JSON}}", serialize(input.turnScores()))
                .replace("{{SCORES_BY_CATEGORY_JSON}}", serialize(input.scoresByCategory()))
                .replace("{{APPLIED_RUBRICS}}", serialize(input.appliedRubrics()))
                .replace("{{DELIVERY_ANALYSIS_JSON}}", nullSafe(input.deliveryAnalysis()))
                .replace("{{VISION_ANALYSIS_JSON}}", nullSafe(input.visionAnalysis()))
                .replace("{{NONVERBAL_AGGREGATE_JSON}}", nullSafe(input.nonverbalAggregate()));
    }

    private String serialize(Object obj) {
        if (obj == null) return "null";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("직렬화 실패: {}", e.getMessage());
            return "null";
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "null";
    }
}
