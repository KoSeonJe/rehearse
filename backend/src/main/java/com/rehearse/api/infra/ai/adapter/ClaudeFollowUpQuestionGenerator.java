package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.models.service.FollowUpQuestionGenerator;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.ClaudeFollowUpQuestionGeneratorClient;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.prompt.FollowUpQuestionPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${claude.api-key:}'.isEmpty()")
public class ClaudeFollowUpQuestionGenerator implements FollowUpQuestionGenerator {

    private static final String JSON_OBJECT_INSTRUCTION =
            "You MUST respond with a single JSON object only. No prose, no markdown, no code fences.";

    private final ClaudeFollowUpQuestionGeneratorClient client;
    private final FollowUpQuestionPromptBuilder promptBuilder;
    private final AiResponseParser aiResponseParser;

    @Override
    public GeneratedFollowUp generate(
            String mainQuestion,
            AnswerAnalysis analysis
    ) {
        FollowUpQuestionPromptBuilder.PromptPair prompt = promptBuilder.build(
                mainQuestion, analysis);
        String systemPrompt = JSON_OBJECT_INSTRUCTION + "\n\n" + prompt.system();
        String content = client.call(systemPrompt, prompt.user());
        return aiResponseParser.parseJsonResponse(content, GeneratedFollowUp.class);
    }
}
