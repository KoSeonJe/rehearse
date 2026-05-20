package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.rubric.models.service.RubricScorer;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.infra.ai.client.ClaudeRubricScorerClient;
import com.rehearse.api.infra.ai.prompt.RubricScorerPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${claude.api-key:}'.isEmpty()")
public class ClaudeRubricScorer implements RubricScorer {

    private static final String JSON_OBJECT_INSTRUCTION =
            "You MUST respond with a single JSON object only. No prose, no markdown, no code fences.";

    private final ClaudeRubricScorerClient client;
    private final RubricScorerPromptBuilder promptBuilder;
    private final RubricScoringPipeline pipeline;

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
        RubricScorerPromptBuilder.PromptBundle prompt = promptBuilder.build(
                question, userAnswer, analysis, rubric, dimensionsToScore, level);

        return pipeline.execute(
                prompt,
                (system, user, isRetry) -> {
                    String systemWithInstruction = JSON_OBJECT_INSTRUCTION + "\n\n" + system;
                    return client.call(systemWithInstruction, user);
                },
                rubric,
                dimensionsToScore,
                userAnswer,
                interviewId,
                questionId
        );
    }
}
