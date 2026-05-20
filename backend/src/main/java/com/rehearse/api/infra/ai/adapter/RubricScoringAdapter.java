package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.adapter.RubricScorerResponseValidator.ValidationResult;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.GeneratedRubricScoring;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RubricScoringAdapter {

    static final String RETRY_FAILED_COUNTER = "rubric_retry_failed_total";
    private static final String STAGE_VERBAL = "verbal";

    private final AiResponseParser responseParser;
    private final ObjectMapper objectMapper;
    private final RubricScorerResponseValidator validator;
    private final MeterRegistry meterRegistry;

    public RubricScoringResult adapt(
            AiClient client,
            ChatRequest request,
            Rubric rubric,
            List<String> dimensionsToScore,
            String userAnswer,
            Long interviewId,
            Long questionId
    ) {
        try {
            ChatResponse firstResponse = client.chat(request);
            Map<String, DimensionScore> firstScores = parseDimensionScores(
                    responseParser.extractJson(firstResponse.content()), dimensionsToScore);

            Map<String, ValidationResult> firstValidation = validateAll(firstScores, dimensionsToScore, userAnswer);
            List<String> retryTargets = collectViolated(firstValidation);

            if (retryTargets.isEmpty()) {
                return buildResult(rubric.rubricId(), dimensionsToScore, firstScores);
            }

            Map<String, DimensionScore> retryScores = runRetry(
                    client, request, dimensionsToScore, retryTargets, firstValidation);
            Map<String, DimensionScore> merged = mergeAfterRetry(
                    dimensionsToScore, firstScores, firstValidation,
                    retryScores, userAnswer, interviewId, questionId);

            return buildResult(rubric.rubricId(), dimensionsToScore, merged);
        } catch (JsonProcessingException parseEx) {
            log.warn("RubricScore JSON 파싱 실패. interviewId={}, questionId={}, reason={}",
                    interviewId, questionId, parseEx.getMessage());
            return buildFallbackScore(rubric.rubricId(), dimensionsToScore, "파싱 실패: " + parseEx.getMessage());
        } catch (Exception ex) {
            log.error("RubricScore 어댑터 예외. interviewId={}, questionId={}, reason={}",
                    interviewId, questionId, ex.getMessage(), ex);
            return buildFallbackScore(rubric.rubricId(), dimensionsToScore, "어댑터 예외: " + ex.getMessage());
        }
    }

    private Map<String, ValidationResult> validateAll(
            Map<String, DimensionScore> scores, List<String> dimensionsToScore, String userAnswer
    ) {
        Map<String, ValidationResult> result = new LinkedHashMap<>();
        for (String dim : dimensionsToScore) {
            DimensionScore ds = scores.get(dim);
            if (ds == null) {
                result.put(dim, ValidationResult.passed());
                continue;
            }
            result.put(dim, validator.validate(dim, ds.score(), ds.observation(), ds.evidenceQuote(), userAnswer));
        }
        return result;
    }

    private List<String> collectViolated(Map<String, ValidationResult> validation) {
        List<String> targets = new ArrayList<>();
        for (Map.Entry<String, ValidationResult> entry : validation.entrySet()) {
            if (!entry.getValue().valid()) {
                targets.add(entry.getKey());
            }
        }
        return targets;
    }

    private Map<String, DimensionScore> runRetry(
            AiClient client, ChatRequest request, List<String> dimensionsToScore,
            List<String> retryTargets, Map<String, ValidationResult> firstValidation
    ) throws JsonProcessingException {
        String hint = buildRetryHint(retryTargets, firstValidation);
        ChatRequest retryRequest = request.withRetryHint(hint, buildSchemaExample(dimensionsToScore));
        ChatResponse retryResponse = client.chat(retryRequest);
        return parseDimensionScores(responseParser.extractJson(retryResponse.content()), dimensionsToScore);
    }

    private Map<String, DimensionScore> mergeAfterRetry(
            List<String> dimensionsToScore,
            Map<String, DimensionScore> firstScores,
            Map<String, ValidationResult> firstValidation,
            Map<String, DimensionScore> retryScores,
            String userAnswer,
            Long interviewId,
            Long questionId
    ) {
        Map<String, DimensionScore> merged = new HashMap<>();
        for (String dim : dimensionsToScore) {
            ValidationResult firstResult = firstValidation.get(dim);
            if (firstResult != null && firstResult.valid()) {
                merged.put(dim, firstScores.get(dim));
                continue;
            }
            DimensionScore retryScore = retryScores.get(dim);
            if (retryScore == null) {
                retryScore = DimensionScore.notApplicable("LLM 응답에 차원 없음");
            }
            ValidationResult retryResult = validator.validate(
                    dim, retryScore.score(), retryScore.observation(), retryScore.evidenceQuote(), userAnswer);
            if (retryResult.valid()) {
                merged.put(dim, retryScore);
                continue;
            }
            recordRetryFailed(dim, retryResult, interviewId, questionId);
            merged.put(dim, DimensionScore.notApplicable("retry_failed: field=" + retryResult.field()));
        }
        return merged;
    }

    private void recordRetryFailed(String dimension, ValidationResult result, Long interviewId, Long questionId) {
        String field = result.field();
        String reason = result.violation() == null ? "unknown" : result.violation().name();
        log.warn("[결함 skip] retry_failed stage={} interviewId={} questionId={} dimension={} field={} reason={}",
                STAGE_VERBAL, interviewId, questionId, dimension, field, reason);
        meterRegistry.counter(RETRY_FAILED_COUNTER,
                "stage", STAGE_VERBAL,
                "dimension", dimension,
                "field", field).increment();
    }

    private String buildRetryHint(List<String> retryTargets, Map<String, ValidationResult> firstValidation) {
        StringBuilder sb = new StringBuilder("일부 차원이 검증 규칙을 위배했습니다. 아래 차원만 룰을 재준수하여 재작성하세요:\n");
        for (String dim : retryTargets) {
            ValidationResult vr = firstValidation.get(dim);
            sb.append("- ").append(dim)
                    .append(": field=").append(vr.field())
                    .append(", reason=").append(vr.violation() == null ? "unknown" : vr.violation().name())
                    .append("\n");
        }
        sb.append("룰: score ∈ {1,2,3}; observation 은 한국어 음절 1+ 포함; evidence_quote 는 사용자 답변 substring 인용 또는 인용할 발언이 전혀 없으면 정확히 \"관련 발언 없음\" 고정 문구.");
        return sb.toString();
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
            Integer score = entry.get("score") == null ? null : ((Number) entry.get("score")).intValue();
            String observation = (String) entry.getOrDefault("observation", "");
            String evidenceQuote = (String) entry.get("evidence_quote");
            result.put(dim, DimensionScore.of(score, observation, evidenceQuote));
        }
        return result;
    }

    private RubricScoringResult buildResult(
            String rubricId, List<String> dimensionsToScore, Map<String, DimensionScore> scores
    ) {
        Map<String, DimensionScore> finalScores = new HashMap<>();
        List<String> scored = new ArrayList<>();
        for (String dim : dimensionsToScore) {
            DimensionScore ds = scores.get(dim);
            if (ds == null) {
                finalScores.put(dim, DimensionScore.notApplicable("LLM 응답에 차원 없음"));
                continue;
            }
            finalScores.put(dim, ds);
            if (ds.score() != null) {
                scored.add(dim);
            }
        }
        GeneratedRubricScoring generated = new GeneratedRubricScoring(rubricId, scored, finalScores, null);
        return generated.toDomain();
    }

    private RubricScoringResult buildFallbackScore(String rubricId, List<String> dimensionsToScore, String reason) {
        Map<String, DimensionScore> fallback = new HashMap<>();
        for (String dim : dimensionsToScore) {
            fallback.put(dim, DimensionScore.notApplicable(reason));
        }
        return new RubricScoringResult(rubricId, Collections.emptyList(), Map.copyOf(fallback), null);
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
}
