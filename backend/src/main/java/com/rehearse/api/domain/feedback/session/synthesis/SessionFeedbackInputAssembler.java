package com.rehearse.api.domain.feedback.session.synthesis;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.service.NonverbalImprovementActionsLoader;
import com.rehearse.api.domain.feedback.score.entity.QuestionScore;
import com.rehearse.api.domain.feedback.score.entity.QuestionScoreDimension;
import com.rehearse.api.domain.feedback.score.repository.QuestionScoreDimensionRepository;
import com.rehearse.api.domain.feedback.score.repository.QuestionScoreRepository;
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

    private static final String NONVERBAL_RUBRIC_ID = "nonverbal-v1";

    private final QuestionScoreRepository questionScoreRepository;
    private final QuestionScoreDimensionRepository questionScoreDimensionRepository;
    private final InterviewFinder interviewFinder;
    private final NonverbalImprovementActionsLoader nonverbalImprovementActionsLoader;

    public SessionFeedbackInput assemble(Long interviewId) {
        Interview interview = interviewFinder.findById(interviewId);
        List<QuestionScore> allScores = questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(interviewId);

        List<QuestionScore> rubricScores = allScores.stream()
                .filter(qs -> !NONVERBAL_RUBRIC_ID.equals(qs.getRubricId()))
                .toList();

        Map<Long, List<QuestionScoreDimension>> dimsByScoreId = loadDimensionsByScoreId(rubricScores);
        List<TurnScoreView> turnScores = rubricScores.stream()
                .map(qs -> toTurnScoreView(qs, dimsByScoreId.getOrDefault(qs.getId(), Collections.emptyList())))
                .toList();

        List<TurnScoreView> okTurns = turnScores.stream()
                .filter(t -> t.status() == TurnScoreView.TurnStatus.OK)
                .toList();

        Map<String, Map<String, Double>> scoresByCategory = buildScoresByCategory(okTurns);
        List<String> appliedRubrics = extractAppliedRubrics(rubricScores);
        String coverage = buildCoverage(turnScores);
        SessionFeedbackInput.SessionMetadata sessionMetadata = buildSessionMetadata(interview, rubricScores.size());

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
        List<QuestionScore> allScores = questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(interviewId);
        List<QuestionScore> nonverbalScores = allScores.stream()
                .filter(qs -> NONVERBAL_RUBRIC_ID.equals(qs.getRubricId()))
                .toList();
        Map<Long, List<QuestionScoreDimension>> nonverbalDimsByScoreId = loadDimensionsByScoreId(nonverbalScores);

        SessionFeedbackInput.NonverbalDeliveryAggregate resolvedNonverbalAggregate =
                buildNonverbalAggregate(nonverbalScores, nonverbalDimsByScoreId);
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

    private SessionFeedbackInput.NonverbalDeliveryAggregate buildNonverbalAggregate(
            List<QuestionScore> nonverbalScores,
            Map<Long, List<QuestionScoreDimension>> dimsByScoreId) {
        if (nonverbalScores.isEmpty()) {
            return null;
        }

        List<SessionFeedbackInput.NonverbalTurnAggregate> turns = nonverbalScores.stream()
                .map(qs -> toNonverbalTurn(qs, dimsByScoreId.getOrDefault(qs.getId(), Collections.emptyList())))
                .toList();

        Map<String, Double> averageScores = buildNonverbalAverageScores(turns);
        SessionFeedbackInput.LowestDimension lowestDimension = findLowestDimension(averageScores);

        return new SessionFeedbackInput.NonverbalDeliveryAggregate(
                "nonverbal_score",
                turns,
                averageScores,
                lowestDimension,
                averageContextMultiplier(turns),
                List.of(new SessionFeedbackInput.RecommendedAction(
                        lowestDimension.dimension(),
                        nonverbalImprovementActionsLoader.resolve(
                                lowestDimension.dimension(),
                                lowestDimension.averageScore()
                        )
                ))
        );
    }

    private SessionFeedbackInput.NonverbalTurnAggregate toNonverbalTurn(QuestionScore qs,
                                                                          List<QuestionScoreDimension> dims) {
        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        for (QuestionScoreDimension dim : dims) {
            dimensionScores.put(dim.getDimensionRef(), dim.getScore());
        }
        return new SessionFeedbackInput.NonverbalTurnAggregate(
                qs.getQuestionId(),
                dimensionScores,
                1.0
        );
    }

    private Map<String, Double> buildNonverbalAverageScores(List<SessionFeedbackInput.NonverbalTurnAggregate> turns) {
        Map<String, List<Integer>> byDim = new LinkedHashMap<>();
        for (SessionFeedbackInput.NonverbalTurnAggregate turn : turns) {
            turn.scores().forEach((dim, score) -> {
                if (score != null) {
                    byDim.computeIfAbsent(dim, k -> new ArrayList<>()).add(score);
                }
            });
        }
        Map<String, Double> averages = new LinkedHashMap<>();
        byDim.forEach((dim, scores) -> averages.put(dim, round1(
                scores.stream().mapToInt(Integer::intValue).average().orElse(0.0)
        )));
        return averages;
    }

    private SessionFeedbackInput.LowestDimension findLowestDimension(Map<String, Double> averages) {
        return averages.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(entry -> new SessionFeedbackInput.LowestDimension(entry.getKey(), entry.getValue()))
                .orElse(new SessionFeedbackInput.LowestDimension("fluency", 0.0));
    }

    private double averageContextMultiplier(List<SessionFeedbackInput.NonverbalTurnAggregate> turns) {
        return turns.stream()
                .mapToDouble(SessionFeedbackInput.NonverbalTurnAggregate::contextMultiplier)
                .average()
                .orElse(1.0);
    }

    private double round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private TurnScoreView toTurnScoreView(QuestionScore qs, List<QuestionScoreDimension> dims) {
        Map<String, DimensionScore> scores = dims.stream()
                .collect(Collectors.toMap(
                        QuestionScoreDimension::getDimensionRef,
                        d -> DimensionScore.of(d.getScore(), d.getObservation(), d.getEvidenceQuote())
                ));

        boolean failed = scores.isEmpty();
        TurnScoreView.TurnStatus status = failed ? TurnScoreView.TurnStatus.FAILED : TurnScoreView.TurnStatus.OK;
        List<String> scoredDimensions = failed ? Collections.emptyList() : new ArrayList<>(scores.keySet());

        return new TurnScoreView(
                qs.getQuestionId(),
                qs.getRubricId(),
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

    private List<String> extractAppliedRubrics(List<QuestionScore> entities) {
        return entities.stream()
                .map(QuestionScore::getRubricId)
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

    private Map<Long, List<QuestionScoreDimension>> loadDimensionsByScoreId(List<QuestionScore> scores) {
        if (scores.isEmpty()) {
            return Map.of();
        }
        List<Long> scoreIds = scores.stream().map(QuestionScore::getId).distinct().toList();
        return questionScoreDimensionRepository.findByQuestionScoreIdIn(scoreIds).stream()
                .collect(Collectors.groupingBy(QuestionScoreDimension::getQuestionScoreId));
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
