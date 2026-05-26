package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.adapter.RubricScoringAdapter;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.prompt.RubricScorerPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RubricScorer {

    private static final int BLANK_ANSWER_LENGTH_THRESHOLD = 3;

    private final RubricCatalog rubricLoader;
    private final RubricScorerPromptBuilder promptBuilder;
    private final RubricScoringAdapter adapter;
    private final AiClient aiClient;

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

        if (isBlankAnswer(userAnswer, analysis)) {
            int length = userAnswer == null ? 0 : userAnswer.strip().length();
            int transcriptLength = analysis == null ? 0 : analysis.transcript().strip().length();
            log.info("무응답 감지 - 전 차원 NOT_EVALUABLE 반환: interviewId={}, questionId={}, len={}, transcriptLen={}",
                    interview.getId(), question.getId(), length, transcriptLength);
            return RubricScoringResult.notEvaluable(rubric.rubricId(), dimensionsToScore,
                    "응답 길이 " + BLANK_ANSWER_LENGTH_THRESHOLD + "자 이하");
        }

        InterviewLevel userLevel = interview.getLevel();

        log.debug("RubricScorer 채점 시작: rubricId={}, dimensions={}",
                rubric.rubricId(), dimensionsToScore);

        ChatRequest request = promptBuilder.build(
                question, userAnswer, analysis, rubric, dimensionsToScore, userLevel
        );

        return adapter.adapt(aiClient, request, rubric, dimensionsToScore,
                userAnswer, interview.getId(), question.getId());
    }

    private boolean isBlankAnswer(String userAnswer, AnswerAnalysis analysis) {
        String effective = effectiveText(userAnswer, analysis);
        return effective == null || effective.strip().length() <= BLANK_ANSWER_LENGTH_THRESHOLD;
    }

    private static String effectiveText(String userAnswer, AnswerAnalysis analysis) {
        if (userAnswer != null && userAnswer.strip().length() > BLANK_ANSWER_LENGTH_THRESHOLD) {
            return userAnswer;
        }
        return analysis != null ? analysis.transcript() : null;
    }
}
