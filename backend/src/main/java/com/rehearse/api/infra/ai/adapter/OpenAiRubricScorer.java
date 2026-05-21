package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.rubric.models.service.RubricScorer;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.infra.ai.client.OpenAiRubricScorerClient;
import com.rehearse.api.infra.ai.prompt.RubricScorerPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiRubricScorer implements RubricScorer {

    private final OpenAiRubricScorerClient client;
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
                (system, user, isRetry) -> client.call(system, user, prompt.jsonSchema()),
                rubric,
                dimensionsToScore,
                userAnswer,
                interviewId,
                questionId
        );
    }
}
