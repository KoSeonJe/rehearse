package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.rubric.models.service.RubricScorer;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RubricScoringService {

    private final RubricCatalog rubricLoader;
    private final RubricScorer rubricScorer;

    public RubricScoringResult score(
            Question question,
            QuestionSet questionSet,
            Interview interview,
            String userAnswer,
            AnswerAnalysis analysis
    ) {
        Rubric rubric = rubricLoader.resolveFor(question, questionSet, interview);
        List<String> dimensionsToScore = rubric.selectDimensions();

        if (dimensionsToScore.isEmpty()) {
            log.debug("채점 차원 없음 — empty RubricScore 반환: rubricId={}", rubric.rubricId());
            return RubricScoringResult.empty(rubric.rubricId());
        }

        InterviewLevel userLevel = interview.getLevel();

        log.debug("RubricScoringService 채점 시작: rubricId={}, dimensions={}",
                rubric.rubricId(), dimensionsToScore);

        return rubricScorer.score(
                question, userAnswer, analysis, rubric,
                dimensionsToScore, userLevel,
                interview.getId(), question.getId()
        );
    }
}
