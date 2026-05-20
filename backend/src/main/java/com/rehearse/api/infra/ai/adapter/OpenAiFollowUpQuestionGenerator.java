package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.models.service.FollowUpQuestionGenerator;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.OpenAiFollowUpQuestionGeneratorClient;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.prompt.FollowUpQuestionPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiFollowUpQuestionGenerator implements FollowUpQuestionGenerator {

    private final OpenAiFollowUpQuestionGeneratorClient client;
    private final FollowUpQuestionPromptBuilder promptBuilder;
    private final AiResponseParser aiResponseParser;

    @Override
    public GeneratedFollowUp generate(
            String mainQuestion,
            String userAnswer,
            AnswerAnalysis analysis,
            ResumeSkeleton resumeSkeleton
    ) {
        FollowUpQuestionPromptBuilder.PromptPair prompt = promptBuilder.build(
                mainQuestion, userAnswer, analysis, resumeSkeleton);
        String content = client.call(prompt.system(), prompt.user());
        GeneratedFollowUp parsed = aiResponseParser.parseJsonResponse(content, GeneratedFollowUp.class);
        return parsed.withAnswerText(userAnswer);
    }
}
