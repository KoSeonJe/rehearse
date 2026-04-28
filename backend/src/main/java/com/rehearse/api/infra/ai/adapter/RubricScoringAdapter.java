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

    private static final int SCORE_MIN = 1;
    private static final int SCORE_MAX = 3;

    private RubricScore parseRubricScore(
            ChatResponse response, AiClient client, ChatRequest request,
            Rubric rubric, List<String> dimensionsToScore
    ) {
        try {
            String json = responseParser.extractJson(response.content());
            Map<String, DimensionScore> scores = parseDimensionScores(json, dimensionsToScore);

            if (hasMissingEvidenceQuote(scores, dimensionsToScore)) {
                log.warn("evidence_quote 누락 차원 존재 — schema retry 1회 시도");
                return retryForEvidenceQuote(client, request, rubric, dimensionsToScore);
            }

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

    private boolean hasMissingEvidenceQuote(Map<String, DimensionScore> scores, List<String> dimensionsToScore) {
        return dimensionsToScore.stream()
                .map(scores::get)
                .anyMatch(ds -> ds != null && ds.score() != null && ds.evidenceQuote() == null);
    }

    private RubricScore retryForEvidenceQuote(
            AiClient client, ChatRequest request, Rubric rubric, List<String> dimensionsToScore
    ) {
        try {
            ChatResponse retryResponse = client.chat(
                    request.withSchemaRetryHint("evidence_quote field is missing", buildSchemaExample(dimensionsToScore)));
            String retryJson = responseParser.extractJson(retryResponse.content());
            Map<String, DimensionScore> retryScores = parseDimensionScores(retryJson, dimensionsToScore);

            // 재시도 후에도 evidence_quote null이면 score null + observation에 사유 기록
            Map<String, DimensionScore> finalScores = new HashMap<>(retryScores);
            for (String dim : dimensionsToScore) {
                DimensionScore ds = finalScores.get(dim);
                if (ds != null && ds.score() != null && ds.evidenceQuote() == null) {
                    finalScores.put(dim, DimensionScore.of(null,
                            ds.observation() + " [evidence_quote 누락으로 score 무효화]", null));
                }
            }
            return buildRubricScore(rubric.rubricId(), dimensionsToScore, finalScores);
        } catch (Exception e) {
            log.error("evidence_quote retry 실패: {}", e.getMessage());
            return buildFallbackScore(rubric.rubricId(), dimensionsToScore, "evidence_quote retry 실패: " + e.getMessage());
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
            Integer score = parseAndValidateScore(dim, entry);
            String observation = (String) entry.getOrDefault("observation", "");
            String evidenceQuote = (String) entry.get("evidence_quote");
            result.put(dim, DimensionScore.of(score, observation, evidenceQuote));
        }

        for (String key : raw.keySet()) {
            if (!dimensionsToScore.contains(key)) {
                log.debug("요청하지 않은 차원 {} 응답에서 제거", key);
            }
        }

        return result;
    }

    private Integer parseAndValidateScore(String dim, Map<String, Object> entry) {
        if (entry.get("score") == null) {
            return null;
        }
        int score = ((Number) entry.get("score")).intValue();
        if (score < SCORE_MIN || score > SCORE_MAX) {
            log.warn("차원 {} score={} 범위(1~3) 초과 — null 처리", dim, score);
            return null;
        }
        return score;
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
