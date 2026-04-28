package com.rehearse.api.domain.feedback.rubric;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import com.rehearse.api.domain.resume.domain.ResumeMode;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.adapter.RubricScoringAdapter;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.prompt.RubricScorerPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RubricScorer {

    private final RubricLoader rubricLoader;
    private final RubricScorerPromptBuilder promptBuilder;
    private final RubricScoringAdapter adapter;
    private final AiClient aiClient;

    public RubricScore score(
            Question question,
            QuestionSet questionSet,
            Interview interview,
            String userAnswer,
            AnswerAnalysis analysis,
            IntentType intent,
            @Nullable ResumeMode resumeMode,
            @Nullable Integer currentChainLevel,
            @Nullable ResumeSkeleton resumeSkeleton
    ) {
        Rubric rubric = rubricLoader.resolveFor(question, questionSet, interview);
        List<String> dimensionsToScore = rubric.selectDimensions(intent, resumeMode);

        if (dimensionsToScore.isEmpty()) {
            log.debug("채점 차원 없음 — empty RubricScore 반환: rubricId={}, intent={}, resumeMode={}",
                    rubric.rubricId(), intent, resumeMode);
            return RubricScore.empty(rubric.rubricId());
        }

        InterviewLevel userLevel = interview.getLevel();

        log.debug("RubricScorer 채점 시작: rubricId={}, dimensions={}, intent={}, mode={}",
                rubric.rubricId(), dimensionsToScore, intent, resumeMode);

        ChatRequest request = promptBuilder.build(
                question, userAnswer, analysis, rubric, dimensionsToScore,
                userLevel, resumeMode, currentChainLevel, resumeSkeleton
        );

        return adapter.adapt(aiClient, request, rubric, dimensionsToScore);
    }
}
