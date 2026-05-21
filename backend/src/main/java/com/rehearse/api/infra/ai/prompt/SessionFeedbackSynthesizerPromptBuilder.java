package com.rehearse.api.infra.ai.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionFeedbackSynthesizerPromptBuilder {

    private static final String TEMPLATE_PATH = "classpath:prompts/template/session-feedback-synthesizer.txt";
    private static final String SYSTEM_PROMPT =
            "You synthesize structured interview feedback in strict JSON. No prose, no markdown, no code fences.";

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    private String template;

    @PostConstruct
    void init() throws IOException {
        var resource = resourceLoader.getResource(TEMPLATE_PATH);
        template = resource.getContentAsString(StandardCharsets.UTF_8);
        log.info("SessionFeedbackSynthesizerPromptBuilder 초기화 완료");
    }

    public PromptPair build(SessionFeedbackInput input) {
        return new PromptPair(SYSTEM_PROMPT, buildUserPrompt(input));
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
                .replace("{{NONVERBAL_AGGREGATE_JSON}}", nonverbalAggregateJson(input));
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

    private String nonverbalAggregateJson(SessionFeedbackInput input) {
        if (input.nonverbalAggregate() != null) {
            return serialize(input.nonverbalAggregate());
        }
        return nullSafe(input.legacyNonverbalAggregateJson());
    }

    public record PromptPair(String system, String user) {
    }
}
