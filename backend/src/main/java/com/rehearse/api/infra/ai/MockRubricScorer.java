package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.score.entity.DimensionStatus;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.rubric.models.service.RubricScorer;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.infra.ai.adapter.ClaudeRubricScorer;
import com.rehearse.api.infra.ai.adapter.OpenAiRubricScorer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnMissingBean({OpenAiRubricScorer.class, ClaudeRubricScorer.class})
public class MockRubricScorer implements RubricScorer {

    @PostConstruct
    void init() {
        log.warn("=== MockRubricScorer 활성화: API 키 없이 Mock 루브릭 점수로 동작합니다 ===");
    }

    @Override
    public RubricScoringResult score(
            Question question,
            String userAnswer,
            AnswerAnalysis analysis,
            Rubric rubric,
            List<String> dimensionsToScore,
            InterviewLevel level,
            Long interviewId,
            Long questionId
    ) {
        Map<String, DimensionScore> scores = new HashMap<>();
        List<String> scored = new ArrayList<>();
        for (String dim : dimensionsToScore) {
            scores.put(dim, new DimensionScore(2, "Mock 관찰 — API 키 미설정 환경 fallback", "Mock evidence", DimensionStatus.OK));
            scored.add(dim);
        }
        return new RubricScoringResult(rubric.rubricId(), scored, Map.copyOf(scores), null);
    }
}
