package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.question.models.service.StandardQuestionGenerator;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.ClaudeQuestionGeneratorClient;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.GeneratedQuestionsWrapper;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${claude.api-key:}'.isEmpty()")
public class ClaudeStandardQuestionGenerator implements StandardQuestionGenerator {

    private static final String JSON_OBJECT_INSTRUCTION =
            "You MUST respond with a single JSON object only. No prose, no markdown, no code fences.";

    private final ClaudeQuestionGeneratorClient client;
    private final QuestionGenerationPromptBuilder promptBuilder;
    private final AiResponseParser aiResponseParser;

    @Override
    public List<GeneratedQuestion> generate(QuestionGenerationRequest request) {
        String systemPrompt = JSON_OBJECT_INSTRUCTION + "\n\n" + promptBuilder.buildSystemPrompt(request);
        String userPrompt = promptBuilder.buildUserPrompt(request);

        String content = client.call(systemPrompt, userPrompt);
        GeneratedQuestionsWrapper wrapper = aiResponseParser.parseJsonResponse(content, GeneratedQuestionsWrapper.class);
        return wrapper.questions();
    }
}
