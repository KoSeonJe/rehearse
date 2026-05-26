package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.adapter.RubricScorerResponseValidator.ValidationResult;
import com.rehearse.api.infra.ai.dto.GeneratedRubricScoring;
import com.rehearse.api.infra.ai.prompt.RubricScorerPromptBuilder;
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

/**
 * RubricScorer adapter 공용 채점 로직 (검증 / retry / merge / fallback).
 * OpenAI / Claude adapter 가 LLM 호출 함수만 주입하여 재사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RubricScoringPipeline {

    public static final String RETRY_FAILED_COUNTER = "rubric_retry_failed_total";
    private static final String STAGE_VERBAL = "verbal";

    private final AiResponseParser responseParser;
    private final ObjectMapper objectMapper;
    private final RubricScorerResponseValidator validator;
    private final RubricScorerPromptBuilder promptBuilder;
    private final MeterRegistry meterRegistry;

    public RubricScoringResult execute(
            RubricScorerPromptBuilder.PromptBundle prompt,
            LlmCaller caller,
            Rubric rubric,
            List<String> dimensionsToScore,
            String transcript,
            Long interviewId,
            Long questionId
    ) {
        try {
            String firstContent = caller.call(prompt.system(), prompt.user(), false);
            Map<String, DimensionScore> firstScores = parseDimensionScores(
                    responseParser.extractJson(firstContent), dimensionsToScore);

            Map<String, ValidationResult> firstValidation = validateAll(
                    firstScores, dimensionsToScore, transcript);
            List<String> retryTargets = collectViolated(firstValidation);

            if (retryTargets.isEmpty()) {
                return buildResult(rubric.rubricId(), dimensionsToScore, firstScores);
            }

            Map<String, DimensionScore> retryScores = runRetry(
                    prompt, caller, dimensionsToScore, retryTargets, firstValidation);
            Map<String, DimensionScore> merged = mergeAfterRetry(
                    dimensionsToScore, firstScores, firstValidation,
                    retryScores, transcript, interviewId, questionId);

            return buildResult(rubric.rubricId(), dimensionsToScore, merged);
        } catch (JsonProcessingException parseEx) {
            log.warn("RubricScore JSON 파싱 실패. interviewId={}, questionId={}, reason={}",
                    interviewId, questionId, parseEx.getMessage());
            return buildFallbackScore(rubric.rubricId(), dimensionsToScore,
                    "파싱 실패: " + parseEx.getMessage());
        }
    }

    private Map<String, ValidationResult> validateAll(
            Map<String, DimensionScore> scores, List<String> dimensionsToScore, String transcript
    ) {
        Map<String, ValidationResult> result = new LinkedHashMap<>();
        for (String dim : dimensionsToScore) {
            DimensionScore ds = scores.get(dim);
            if (ds == null) {
                result.put(dim, ValidationResult.passed());
                continue;
            }
            result.put(dim, validator.validate(dim, ds.score(), ds.observation(), ds.evidenceQuote(), transcript));
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
            RubricScorerPromptBuilder.PromptBundle prompt,
            LlmCaller caller,
            List<String> dimensionsToScore,
            List<String> retryTargets,
            Map<String, ValidationResult> firstValidation
    ) throws JsonProcessingException {
        Map<String, String> targetReasons = new LinkedHashMap<>();
        for (String dim : retryTargets) {
            ValidationResult vr = firstValidation.get(dim);
            String reason = "field=" + vr.field()
                    + ", reason=" + (vr.violation() == null ? "unknown" : vr.violation().name());
            targetReasons.put(dim, reason);
        }
        String hint = promptBuilder.buildRetryHint(retryTargets, targetReasons, dimensionsToScore);
        String retryUser = prompt.user() + "\n\n" + hint;
        String retryContent = caller.call(prompt.system(), retryUser, true);
        return parseDimensionScores(responseParser.extractJson(retryContent), dimensionsToScore);
    }

    private Map<String, DimensionScore> mergeAfterRetry(
            List<String> dimensionsToScore,
            Map<String, DimensionScore> firstScores,
            Map<String, ValidationResult> firstValidation,
            Map<String, DimensionScore> retryScores,
            String transcript,
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
                    dim, retryScore.score(), retryScore.observation(), retryScore.evidenceQuote(), transcript);
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
            result.put(dim, toDimensionScore(entry));
        }
        return result;
    }

    private DimensionScore toDimensionScore(Map<String, Object> entry) {
        Integer score = entry.get("score") == null ? null : ((Number) entry.get("score")).intValue();
        String observation = (String) entry.getOrDefault("observation", "");
        String evidenceQuote = (String) entry.get("evidence_quote");
        if (score == null && isNotEvaluableSentinel(observation)) {
            return DimensionScore.notEvaluable(RubricScorerResponseValidator.NO_RELATED_UTTERANCE);
        }
        return DimensionScore.of(score, observation, evidenceQuote);
    }

    private boolean isNotEvaluableSentinel(String observation) {
        return observation != null
                && observation.strip().startsWith(RubricScorerResponseValidator.NO_RELATED_UTTERANCE);
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

    @FunctionalInterface
    public interface LlmCaller {
        /**
         * @param isRetry true 인 경우 1차 위배 후 retry hint 가 포함된 user prompt 가 전달됨.
         *                provider 별로 schema 파라미터를 retry 단계에서 생략하는 등의 분기에 활용 가능.
         */
        String call(String systemPrompt, String userPrompt, boolean isRetry) throws JsonProcessingException;
    }
}
