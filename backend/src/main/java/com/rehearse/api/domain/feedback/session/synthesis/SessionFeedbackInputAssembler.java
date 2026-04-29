package com.rehearse.api.domain.feedback.session.synthesis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.NonverbalScore;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScore;
import com.rehearse.api.domain.feedback.rubric.repository.NonverbalScoreRepository;
import com.rehearse.api.domain.feedback.rubric.repository.RubricScoreRepository;
import com.rehearse.api.domain.feedback.rubric.service.NonverbalImprovementActionsLoader;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SessionFeedbackInputAssembler {

    private final RubricScoreRepository rubricScoreRepository;
    private final NonverbalScoreRepository nonverbalScoreRepository;
    private final InterviewFinder interviewFinder;
    private final ObjectMapper objectMapper;
    private final NonverbalImprovementActionsLoader nonverbalImprovementActionsLoader;

    public SessionFeedbackInput assemble(Long interviewId) {
        Interview interview = interviewFinder.findById(interviewId);
        List<RubricScore> scoreEntities = rubricScoreRepository.findByInterviewIdOrderByTurnIdAsc(interviewId);

        List<TurnScoreView> turnScores = scoreEntities.stream()
                .map(this::toTurnScoreView)
                .toList();

        List<TurnScoreView> okTurns = turnScores.stream()
                .filter(t -> t.status() == TurnScoreView.TurnStatus.OK)
                .toList();

        Map<String, Map<String, Double>> scoresByCategory = buildScoresByCategory(okTurns);
        List<String> appliedRubrics = extractAppliedRubrics(scoreEntities);
        String coverage = buildCoverage(turnScores);
        Object sessionMetadata = buildSessionMetadata(interview, scoreEntities.size());

        return new SessionFeedbackInput(
                sessionMetadata,
                turnScores,
                scoresByCategory,
                appliedRubrics,
                null,
                null,
                null,
                coverage,
                interview.getLevel()
        );
    }

    public SessionFeedbackInput assembleWithDelivery(Long interviewId, String deliveryAnalysis,
                                                      String visionAnalysis, String nonverbalAggregate) {
        SessionFeedbackInput base = assemble(interviewId);
        String resolvedNonverbalAggregate = buildNonverbalAggregate(interviewId, nonverbalAggregate);
        return new SessionFeedbackInput(
                base.sessionMetadata(),
                base.turnScores(),
                base.scoresByCategory(),
                base.appliedRubrics(),
                deliveryAnalysis,
                visionAnalysis,
                resolvedNonverbalAggregate,
                base.coverage(),
                base.userLevel()
        );
    }

    private String buildNonverbalAggregate(Long interviewId, String fallbackAggregate) {
        List<NonverbalScore> scores = nonverbalScoreRepository.findByInterviewIdOrderByTurnIdAsc(interviewId);
        if (scores.isEmpty()) {
            return fallbackAggregate;
        }

        Map<String, Double> averageScores = buildNonverbalAverageScores(scores);
        LowestDimension lowestDimension = findLowestDimension(averageScores);

        Map<String, Object> aggregate = new LinkedHashMap<>();
        aggregate.put("source", "nonverbal_score");
        aggregate.put("turns", scores.stream().map(this::toNonverbalTurn).toList());
        aggregate.put("averageScores", averageScores);
        aggregate.put("lowestDimension", lowestDimension.toMap());
        aggregate.put("averageContextMultiplier", averageContextMultiplier(scores));
        aggregate.put("recommendedActions", List.of(Map.of(
                "dimension", lowestDimension.dimension(),
                "actions", nonverbalImprovementActionsLoader.resolve(
                        lowestDimension.dimension(),
                        lowestDimension.averageScore()
                )
        )));
        Object legacyAggregate = parseJsonOrRaw(fallbackAggregate);
        if (legacyAggregate != null) {
            aggregate.put("legacyAggregate", legacyAggregate);
        }
        return writeJson(aggregate);
    }

    private Map<String, Object> toNonverbalTurn(NonverbalScore score) {
        Map<String, Object> turn = new LinkedHashMap<>();
        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put("D11", score.getFluencyScore());
        dimensionScores.put("D12", score.getToneScore());
        dimensionScores.put("D13", score.getPostureScore());
        dimensionScores.put("D14", score.getComposureScore());

        turn.put("turnId", score.getTurnId());
        turn.put("scores", dimensionScores);
        turn.put("contextMultiplier", toDouble(score.getContextMultiplier()));
        return turn;
    }

    private Map<String, Double> buildNonverbalAverageScores(List<NonverbalScore> scores) {
        Map<String, Double> averages = new LinkedHashMap<>();
        averages.put("D11", average(scores.stream().map(NonverbalScore::getFluencyScore).toList()));
        averages.put("D12", average(scores.stream().map(NonverbalScore::getToneScore).toList()));
        averages.put("D13", average(scores.stream().map(NonverbalScore::getPostureScore).toList()));
        averages.put("D14", average(scores.stream().map(NonverbalScore::getComposureScore).toList()));
        return averages;
    }

    private LowestDimension findLowestDimension(Map<String, Double> averages) {
        return averages.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(entry -> new LowestDimension(entry.getKey(), entry.getValue()))
                .orElse(new LowestDimension("D11", 0.0));
    }

    private double average(List<Integer> scores) {
        double value = scores.stream()
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        return round1(value);
    }

    private double averageContextMultiplier(List<NonverbalScore> scores) {
        double value = scores.stream()
                .map(NonverbalScore::getContextMultiplier)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(1.0);
        return round2(value);
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 1.0 : value.doubleValue();
    }

    private double round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private Object parseJsonOrRaw(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(value);
            return node;
        } catch (JsonProcessingException e) {
            return value;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("SessionFeedback nonverbal aggregate 직렬화 실패", e);
        }
    }

    private TurnScoreView toTurnScoreView(RubricScore entity) {
        Map<String, DimensionScore> scores = entity.getScoresJson();
        boolean failed = scores == null || scores.isEmpty();

        TurnScoreView.TurnStatus status = failed
                ? TurnScoreView.TurnStatus.FAILED
                : TurnScoreView.TurnStatus.OK;

        List<String> scoredDimensions = failed
                ? Collections.emptyList()
                : new ArrayList<>(scores.keySet());

        return new TurnScoreView(
                entity.getTurnId(),
                entity.getRubricId(),
                scoredDimensions,
                failed ? Collections.emptyMap() : scores,
                status
        );
    }

    private Map<String, Map<String, Double>> buildScoresByCategory(List<TurnScoreView> okTurns) {
        Map<String, List<TurnScoreView>> byRubric = okTurns.stream()
                .collect(Collectors.groupingBy(TurnScoreView::rubricId));

        Map<String, Map<String, Double>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<TurnScoreView>> entry : byRubric.entrySet()) {
            String rubricId = entry.getKey();
            List<TurnScoreView> turns = entry.getValue();

            Map<String, List<Integer>> dimensionScoresByKey = new LinkedHashMap<>();
            for (TurnScoreView turn : turns) {
                turn.dimensionScores().forEach((dim, score) -> {
                    if (score != null && score.score() != null) {
                        dimensionScoresByKey
                                .computeIfAbsent(dim, k -> new ArrayList<>())
                                .add(score.score());
                    }
                });
            }

            Map<String, Double> averages = new LinkedHashMap<>();
            dimensionScoresByKey.forEach((dim, scores) -> {
                double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                averages.put(dim, Math.round(avg * 10.0) / 10.0);
            });

            result.put(rubricId, averages);
        }
        return result;
    }

    private List<String> extractAppliedRubrics(List<RubricScore> entities) {
        return entities.stream()
                .map(RubricScore::getRubricId)
                .distinct()
                .toList();
    }

    private String buildCoverage(List<TurnScoreView> turnScores) {
        long failed = turnScores.stream()
                .filter(t -> t.status() == TurnScoreView.TurnStatus.FAILED)
                .count();
        if (failed == 0) {
            return "all turns scored";
        }
        long ok = turnScores.size() - failed;
        return ok + "/" + turnScores.size() + " turns scored";
    }

    private Object buildSessionMetadata(Interview interview, int totalTurns) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("interviewId", interview.getId());
        metadata.put("position", interview.getPosition() != null ? interview.getPosition().name() : "UNKNOWN");
        metadata.put("level", interview.getLevel() != null ? interview.getLevel().name() : "MID");
        metadata.put("interviewTypes", interview.getInterviewTypes().stream()
                .map(Enum::name)
                .toList());
        metadata.put("totalTurns", totalTurns);
        metadata.put("durationMinutes", interview.getDurationMinutes() != null ? interview.getDurationMinutes() : 0);
        return metadata;
    }

    private record LowestDimension(String dimension, double averageScore) {

        private Map<String, Object> toMap() {
            return Map.of(
                    "dimension", dimension,
                    "averageScore", averageScore
            );
        }
    }
}
