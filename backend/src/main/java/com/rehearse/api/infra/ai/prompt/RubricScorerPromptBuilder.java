package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricDimension;
import com.rehearse.api.domain.feedback.rubric.service.RubricCatalog;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.question.entity.Question;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RubricScorerPromptBuilder {

    public static final String SCHEMA_NAME = "rubric_score";
    private static final String TEMPLATE_PATH = "classpath:prompts/template/turn-rubric-scorer.txt";

    private final RubricCatalog rubricLoader;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    private String template;
    private String cachedDimensionDefinitions;

    @PostConstruct
    void init() throws IOException {
        Resource resource = resourceLoader.getResource(TEMPLATE_PATH);
        template = resource.getContentAsString(StandardCharsets.UTF_8);
        cachedDimensionDefinitions = buildDimensionDefinitions();
        log.info("RubricScorerPromptBuilder 초기화 완료");
    }

    public PromptBundle build(
            Question question,
            AnswerAnalysis analysis,
            Rubric rubric,
            List<String> dimensionsToScore,
            InterviewLevel userLevel
    ) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(question, analysis, rubric,
                dimensionsToScore, userLevel);
        Map<String, Object> schema = buildJsonSchema(dimensionsToScore);
        return new PromptBundle(systemPrompt, userPrompt, schema);
    }

    public String buildRetryHint(
            List<String> retryTargets,
            Map<String, String> targetReasons,
            List<String> dimensionsToScore
    ) {
        StringBuilder sb = new StringBuilder("일부 차원이 검증 규칙을 위배했습니다. 아래 차원만 룰을 재준수하여 재작성하세요:\n");
        for (String dim : retryTargets) {
            sb.append("- ").append(dim).append(": ").append(targetReasons.getOrDefault(dim, "unknown")).append("\n");
        }
        sb.append("룰: score ∈ {1,2,3}; observation 은 한국어 음절 1+ 포함; evidence_quote 는 사용자 답변 substring 만 인용.\n");
        sb.append("반드시 아래 형태의 JSON 객체로만 응답하세요.\n```json\n")
                .append(buildSchemaExample(dimensionsToScore))
                .append("\n```");
        return sb.toString();
    }

    Map<String, Object> buildJsonSchema(List<String> dimensionsToScore) {
        Map<String, Object> scoreSchema = new LinkedHashMap<>();
        scoreSchema.put("type", List.of("integer", "null"));
        scoreSchema.put("enum", Arrays.asList(1, 2, 3, null));
        scoreSchema.put("description", "1~3 점 또는 null (차원 무관 시 null)");

        Map<String, Object> observationSchema = new LinkedHashMap<>();
        observationSchema.put("type", "string");
        observationSchema.put("description", "한국어 1~2문장 관찰 서술");

        Map<String, Object> evidenceQuoteSchema = new LinkedHashMap<>();
        evidenceQuoteSchema.put("type", "string");
        evidenceQuoteSchema.put("description", "사용자 답변에서 추출한 verbatim substring (non-null)");

        Map<String, Object> dimensionProperties = new LinkedHashMap<>();
        dimensionProperties.put("score", scoreSchema);
        dimensionProperties.put("observation", observationSchema);
        dimensionProperties.put("evidence_quote", evidenceQuoteSchema);

        Map<String, Object> dimensionSchema = new LinkedHashMap<>();
        dimensionSchema.put("type", "object");
        dimensionSchema.put("additionalProperties", false);
        dimensionSchema.put("required", List.of("score", "observation", "evidence_quote"));
        dimensionSchema.put("properties", dimensionProperties);

        Map<String, Object> properties = new LinkedHashMap<>();
        for (String dim : dimensionsToScore) {
            properties.put(dim, dimensionSchema);
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.copyOf(dimensionsToScore));
        schema.put("properties", properties);
        return schema;
    }

    private String buildSystemPrompt() {
        return "당신은 한국어로 코칭하는 면접 평가자입니다. observation 은 한국어 1~2문장, evidence_quote 는 사용자 답변 substring 만 인용.\n\n" +
                "## 전체 차원 정의 (참고용 — user 메시지의 DIMENSIONS_TO_SCORE 에 명시된 차원만 채점)\n\n" +
                cachedDimensionDefinitions;
    }

    private String buildUserPrompt(
            Question question,
            AnswerAnalysis analysis,
            Rubric rubric,
            List<String> dimensionsToScore,
            InterviewLevel userLevel
    ) {
        return template
                .replace("{{USER_LEVEL}}", formatLevel(userLevel))
                .replace("{{QUESTION_TEXT}}", question.getQuestionText())
                .replace("{{USER_ANSWER}}", analysis != null ? analysis.transcript() : "")
                .replace("{{ANSWER_ANALYSIS_JSON}}", serializeAnalysis(analysis))
                .replace("{{DIMENSIONS_TO_SCORE}}", String.join(", ", dimensionsToScore))
                .replace("{{DIMENSION_DEFINITIONS}}", buildSelectedDefinitions(dimensionsToScore))
                .replace("{{RESUME_CONTEXT}}", "")
                .replace("{{CHAIN_DEPTH_OVERRIDE}}", "");
    }

    private String buildDimensionDefinitions() {
        Map<String, RubricDimension> dimensions = rubricLoader.getAllDimensions();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, RubricDimension> entry : dimensions.entrySet()) {
            RubricDimension dim = entry.getValue();
            sb.append("### ").append(entry.getKey()).append(" — ").append(dim.name()).append("\n");
            sb.append(dim.description()).append("\n");
            if (dim.scoring() != null) {
                for (Map.Entry<Integer, RubricDimension.ScoringLevel> se : dim.scoring().entrySet()) {
                    sb.append("- Score ").append(se.getKey()).append(" (").append(se.getValue().label()).append(")");
                    if (se.getValue().observable() != null && !se.getValue().observable().isEmpty()) {
                        sb.append(": ").append(String.join("; ", se.getValue().observable()));
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildSelectedDefinitions(List<String> dimensionsToScore) {
        Map<String, RubricDimension> dimensions = rubricLoader.getAllDimensions();
        StringBuilder sb = new StringBuilder();
        for (String ref : dimensionsToScore) {
            RubricDimension dim = dimensions.get(ref);
            if (dim == null) {
                continue;
            }
            sb.append("### ").append(ref).append(" — ").append(dim.name()).append("\n");
            sb.append(dim.description()).append("\n");
            if (dim.scoring() != null) {
                for (Map.Entry<Integer, RubricDimension.ScoringLevel> se : dim.scoring().entrySet()) {
                    sb.append("- Score ").append(se.getKey()).append(" (").append(se.getValue().label()).append(")");
                    if (se.getValue().observable() != null && !se.getValue().observable().isEmpty()) {
                        sb.append(": ").append(String.join("; ", se.getValue().observable()));
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatLevel(InterviewLevel level) {
        if (level == null) return "MID";
        return switch (level) {
            case JUNIOR -> "JUNIOR";
            case MID -> "MID";
            case SENIOR -> "SENIOR";
        };
    }

    private String serializeAnalysis(AnswerAnalysis analysis) {
        if (analysis == null) return "{}";
        try {
            return objectMapper.writeValueAsString(analysis);
        } catch (JsonProcessingException e) {
            log.warn("AnswerAnalysis 직렬화 실패", e);
            return "{}";
        }
    }

    private String buildSchemaExample(List<String> dimensionsToScore) {
        if (dimensionsToScore.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{\n");
        for (int i = 0; i < dimensionsToScore.size(); i++) {
            String dim = dimensionsToScore.get(i);
            sb.append("  \"").append(dim).append("\": {\"score\": 2, \"observation\": \"한국어 관찰 문장\", \"evidence_quote\": \"사용자 답변 substring\"}");
            if (i < dimensionsToScore.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    public record PromptBundle(String system, String user, Map<String, Object> jsonSchema) {}
}
