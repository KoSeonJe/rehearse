package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.FocusHints;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;
import com.rehearse.api.infra.ai.dto.JsonSchemaSpec;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import com.rehearse.api.infra.ai.prompt.PromptFormatters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerAnalyzer {

    private static final String CALL_TYPE = "answer_analyzer";
    static final String SCHEMA_NAME = "answer_analysis";
    private static final double TEMPERATURE = 0.2;
    private static final int MAX_TOKENS = 800;

    static final List<String> DIMENSION_KEYS = List.of(
            "problem_framing", "technical_depth", "reasoning_communication",
            "conceptual_accuracy", "practical_application", "experience_concreteness",
            "collaboration_awareness", "recovery_from_gaps",
            "factual_consistency", "chain_depth");

    private static final List<String> EVIDENCE_STRENGTHS = List.of("STRONG", "WEAK", "ASSUMED");
    private static final List<String> RECOMMENDED_ACTIONS = List.of(
            "DEEP_DIVE", "CLARIFICATION", "CHALLENGE", "APPLICATION", "SKIP");

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final InterviewContextBuilder contextBuilder;

    public AnswerAnalysis analyze(
            Long interviewId,
            String mainQuestion,
            ReferenceType questionReferenceType,
            String userAnswer
    ) {
        if (interviewId == null) {
            throw new IllegalArgumentException("interviewId 는 null 일 수 없습니다.");
        }

        String personaDepthHint = PromptFormatters.toReferenceLabel(questionReferenceType);

        BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                CALL_TYPE,
                new FocusHints.AnswerAnalyzerHints(
                        mainQuestion != null ? mainQuestion : "",
                        userAnswer != null ? userAnswer : "",
                        personaDepthHint
                ),
                null
        ));

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(built.messages())
                .callType(CALL_TYPE)
                .temperature(TEMPERATURE)
                .maxTokens(MAX_TOKENS)
                .responseFormat(ResponseFormat.JSON_SCHEMA)
                .jsonSchema(new JsonSchemaSpec(SCHEMA_NAME, buildJsonSchema()))
                .build();

        ChatResponse response = aiClient.chat(chatRequest);
        GeneratedAnswerAnalysis parsed = aiResponseParser.parseOrRetry(
                response, GeneratedAnswerAnalysis.class, aiClient, chatRequest);

        return parsed.toDomain();
    }

    static Map<String, Object> buildJsonSchema() {
        Map<String, Object> claimProps = new LinkedHashMap<>();
        claimProps.put("text", Map.of("type", "string"));
        claimProps.put("depth_score", Map.of("type", "integer"));
        claimProps.put("evidence_strength", Map.of("type", "string", "enum", EVIDENCE_STRENGTHS));
        claimProps.put("topic_tag", Map.of("type", "string"));

        Map<String, Object> claimItem = new LinkedHashMap<>();
        claimItem.put("type", "object");
        claimItem.put("additionalProperties", false);
        claimItem.put("required", List.of("text", "depth_score", "evidence_strength", "topic_tag"));
        claimItem.put("properties", claimProps);

        Map<String, Object> dimensionGapsProps = new LinkedHashMap<>();
        for (String key : DIMENSION_KEYS) {
            dimensionGapsProps.put(key, Map.of("type", List.of("integer", "null")));
        }
        Map<String, Object> dimensionGapsSchema = new LinkedHashMap<>();
        dimensionGapsSchema.put("type", "object");
        dimensionGapsSchema.put("additionalProperties", false);
        dimensionGapsSchema.put("required", List.copyOf(DIMENSION_KEYS));
        dimensionGapsSchema.put("properties", dimensionGapsProps);

        Map<String, Object> rootProps = new LinkedHashMap<>();
        rootProps.put("claims", Map.of("type", "array", "items", claimItem));
        rootProps.put("dimension_gaps", dimensionGapsSchema);
        rootProps.put("weakest_dimension", Map.of("type", List.of("string", "null")));
        rootProps.put("unstated_assumptions",
                Map.of("type", "array", "items", Map.of("type", "string")));
        rootProps.put("recommended_next_action",
                Map.of("type", "string", "enum", RECOMMENDED_ACTIONS));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of(
                "claims", "dimension_gaps", "weakest_dimension",
                "unstated_assumptions", "recommended_next_action"));
        schema.put("properties", rootProps);
        return schema;
    }
}
