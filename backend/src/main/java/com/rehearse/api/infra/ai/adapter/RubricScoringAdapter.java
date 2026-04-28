package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.feedback.rubric.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.Rubric;
import com.rehearse.api.domain.feedback.rubric.RubricScore;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RubricScoringAdapter {

    private final AiResponseParser responseParser;
    private final ObjectMapper objectMapper;

    public RubricScore adapt(AiClient client, ChatRequest request, Rubric rubric, List<String> dimensionsToScore) {
        ChatResponse response = client.chat(request);
        return parseRubricScore(response, client, request, rubric, dimensionsToScore);
    }

    private RubricScore parseRubricScore(
            ChatResponse response, AiClient client, ChatRequest request,
            Rubric rubric, List<String> dimensionsToScore
    ) {
        try {
            String json = responseParser.extractJson(response.content());
            Map<String, DimensionScore> scores = parseDimensionScores(json, dimensionsToScore);
            return buildRubricScore(rubric.rubricId(), dimensionsToScore, scores);
        } catch (Exception firstEx) {
            log.warn("RubricScore 1차 파싱 실패, 재호출 시도: {}", firstEx.getMessage());
            try {
                ChatResponse retryResponse = client.chat(
                        request.withSchemaRetryHint(firstEx.getMessage(), buildSchemaExample(dimensionsToScore)));
                String retryJson = responseParser.extractJson(retryResponse.content());
                Map<String, DimensionScore> scores = parseDimensionScores(retryJson, dimensionsToScore);
                return buildRubricScore(rubric.rubricId(), dimensionsToScore, scores);
            } catch (Exception secondEx) {
                log.error("RubricScore 2차 파싱도 실패: {}", secondEx.getMessage());
                return buildFallbackScore(rubric.rubricId(), dimensionsToScore, "파싱 실패: " + secondEx.getMessage());
            }
        }
    }

    private Map<String, DimensionScore> parseDimensionScores(String json, List<String> dimensionsToScore)
            throws JsonProcessingException {
        TypeReference<Map<String, Map<String, Object>>> typeRef = new TypeReference<>() {};
        Map<String, Map<String, Object>> raw = objectMapper.readValue(json, typeRef);

        Map<String, DimensionScore> result = new HashMap<>();
        for (String dim : dimensionsToScore) {
            Map<String, Object> entry = raw.get(dim);
            if (entry == null) {
                result.put(dim, DimensionScore.notApplicable("LLM 응답에 차원 없음"));
                continue;
            }
            Integer score = entry.get("score") != null ? ((Number) entry.get("score")).intValue() : null;
            String observation = (String) entry.getOrDefault("observation", "");
            String evidenceQuote = (String) entry.get("evidence_quote");
            result.put(dim, DimensionScore.of(score, observation, evidenceQuote));
        }

        // 요청 외 차원은 null 강제 (보안상 요청하지 않은 차원 점수 차단)
        for (String key : raw.keySet()) {
            if (!dimensionsToScore.contains(key)) {
                log.debug("요청하지 않은 차원 {} 응답에서 제거", key);
            }
        }

        return result;
    }

    private RubricScore buildRubricScore(
            String rubricId, List<String> dimensionsToScore, Map<String, DimensionScore> scores
    ) {
        List<String> scored = new ArrayList<>();
        for (String dim : dimensionsToScore) {
            DimensionScore ds = scores.get(dim);
            if (ds != null && ds.score() != null) {
                scored.add(dim);
            }
        }
        return new RubricScore(rubricId, List.copyOf(scored), Map.copyOf(scores), null);
    }

    private RubricScore buildFallbackScore(String rubricId, List<String> dimensionsToScore, String reason) {
        Map<String, DimensionScore> fallback = new HashMap<>();
        for (String dim : dimensionsToScore) {
            fallback.put(dim, DimensionScore.notApplicable(reason));
        }
        return new RubricScore(rubricId, List.of(), Map.copyOf(fallback), null);
    }

    private String buildSchemaExample(List<String> dimensionsToScore) {
        if (dimensionsToScore.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{\n");
        for (int i = 0; i < dimensionsToScore.size(); i++) {
            String dim = dimensionsToScore.get(i);
            sb.append("  \"").append(dim).append("\": {\"score\": 2, \"observation\": \"...\", \"evidence_quote\": \"...\"}");
            if (i < dimensionsToScore.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
