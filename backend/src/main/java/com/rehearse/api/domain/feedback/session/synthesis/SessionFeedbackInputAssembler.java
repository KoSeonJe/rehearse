package com.rehearse.api.domain.feedback.session.synthesis;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoreEntity;
import com.rehearse.api.domain.feedback.rubric.repository.RubricScoreRepository;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SessionFeedbackInputAssembler {

    private final RubricScoreRepository rubricScoreRepository;
    private final InterviewFinder interviewFinder;

    public SessionFeedbackInput assemble(Long interviewId) {
        Interview interview = interviewFinder.findById(interviewId);
        List<RubricScoreEntity> scoreEntities = rubricScoreRepository.findByInterviewIdOrderByTurnIdAsc(interviewId);

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
        return new SessionFeedbackInput(
                base.sessionMetadata(),
                base.turnScores(),
                base.scoresByCategory(),
                base.appliedRubrics(),
                deliveryAnalysis,
                visionAnalysis,
                nonverbalAggregate,
                base.coverage(),
                base.userLevel()
        );
    }

    private TurnScoreView toTurnScoreView(RubricScoreEntity entity) {
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

    private List<String> extractAppliedRubrics(List<RubricScoreEntity> entities) {
        return entities.stream()
                .map(RubricScoreEntity::getRubricId)
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
}
