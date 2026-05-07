package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.adapter.RubricScoringAdapter;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.prompt.RubricScorerPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RubricScorer {

    private final RubricCatalog rubricLoader;
    private final RubricScorerPromptBuilder promptBuilder;
    private final RubricScoringAdapter adapter;
    private final AiClient aiClient;

    public RubricScoringResult score(
            Question question,
            QuestionSet questionSet,
            Interview interview,
            String userAnswer,
            AnswerAnalysis analysis,
            @Nullable ResumeMode resumeMode,
            @Nullable Integer currentChainLevel,
            @Nullable ResumeSkeleton resumeSkeleton
    ) {
        Rubric rubric = rubricLoader.resolveFor(question, questionSet, interview);
        List<String> dimensionsToScore = rubric.selectDimensions(resumeMode);

        if (dimensionsToScore.isEmpty()) {
            log.debug("채점 차원 없음 — empty RubricScore 반환: rubricId={}, resumeMode={}",
                    rubric.rubricId(), resumeMode);
            return RubricScoringResult.empty(rubric.rubricId());
        }

        InterviewLevel userLevel = interview.getLevel();

        log.debug("RubricScorer 채점 시작: rubricId={}, dimensions={}, mode={}",
                rubric.rubricId(), dimensionsToScore, resumeMode);

        ChatRequest request = promptBuilder.build(
                question, userAnswer, analysis, rubric, dimensionsToScore,
                userLevel, resumeMode, currentChainLevel, resumeSkeleton
        );

        return adapter.adapt(aiClient, request, rubric, dimensionsToScore);
    }
}
