package com.rehearse.api.domain.feedback.session.synthesis;

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
        SessionFeedbackInput.SessionMetadata sessionMetadata = buildSessionMetadata(interview, scoreEntities.size());

        return new SessionFeedbackInput(
                sessionMetadata,
                turnScores,
                scoresByCategory,
                appliedRubrics,
                null,
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
        SessionFeedbackInput.NonverbalDeliveryAggregate resolvedNonverbalAggregate =
                buildNonverbalAggregate(interviewId);
        String legacyNonverbalAggregateJson = resolvedNonverbalAggregate == null ? nonverbalAggregate : null;
        return new SessionFeedbackInput(
                base.sessionMetadata(),
                base.turnScores(),
                base.scoresByCategory(),
                base.appliedRubrics(),
                deliveryAnalysis,
                visionAnalysis,
                resolvedNonverbalAggregate,
                legacyNonverbalAggregateJson,
                base.coverage(),
                base.userLevel()
        );
    }

    private SessionFeedbackInput.NonverbalDeliveryAggregate buildNonverbalAggregate(Long interviewId) {
        List<NonverbalScore> scores = nonverbalScoreRepository.findByInterviewIdOrderByTurnIdAsc(interviewId);
        if (scores.isEmpty()) {
            return null;
        }

        Map<String, Double> averageScores = buildNonverbalAverageScores(scores);
        SessionFeedbackInput.LowestDimension lowestDimension = findLowestDimension(averageScores);

        return new SessionFeedbackInput.NonverbalDeliveryAggregate(
                "nonverbal_score",
                scores.stream().map(this::toNonverbalTurn).toList(),
                averageScores,
                lowestDimension,
                averageContextMultiplier(scores),
                List.of(new SessionFeedbackInput.RecommendedAction(
                        lowestDimension.dimension(),
                        nonverbalImprovementActionsLoader.resolve(
                                lowestDimension.dimension(),
                                lowestDimension.averageScore()
                        )
                ))
        );
    }

    private SessionFeedbackInput.NonverbalTurnAggregate toNonverbalTurn(NonverbalScore score) {
        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put("D11", score.getFluencyScore());
        dimensionScores.put("D12", score.getToneScore());
        dimensionScores.put("D13", score.getPostureScore());
        dimensionScores.put("D14", score.getComposureScore());

        return new SessionFeedbackInput.NonverbalTurnAggregate(
                score.getTurnId(),
                dimensionScores,
                toDouble(score.getContextMultiplier())
        );
    }

    private Map<String, Double> buildNonverbalAverageScores(List<NonverbalScore> scores) {
        Map<String, Double> averages = new LinkedHashMap<>();
        averages.put("D11", average(scores.stream().map(NonverbalScore::getFluencyScore).toList()));
        averages.put("D12", average(scores.stream().map(NonverbalScore::getToneScore).toList()));
        averages.put("D13", average(scores.stream().map(NonverbalScore::getPostureScore).toList()));
        averages.put("D14", average(scores.stream().map(NonverbalScore::getComposureScore).toList()));
        return averages;
    }

    private SessionFeedbackInput.LowestDimension findLowestDimension(Map<String, Double> averages) {
        return averages.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(entry -> new SessionFeedbackInput.LowestDimension(entry.getKey(), entry.getValue()))
                .orElse(new SessionFeedbackInput.LowestDimension("D11", 0.0));
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

    private SessionFeedbackInput.SessionMetadata buildSessionMetadata(Interview interview, int totalTurns) {
        return new SessionFeedbackInput.SessionMetadata(
                interview.getId(),
                interview.getPosition() != null ? interview.getPosition().name() : "UNKNOWN",
                interview.getLevel() != null ? interview.getLevel().name() : "MID",
                interview.getInterviewTypes().stream()
                        .map(Enum::name)
                        .toList(),
                totalTurns,
                interview.getDurationMinutes() != null ? interview.getDurationMinutes() : 0
        );
    }
}
