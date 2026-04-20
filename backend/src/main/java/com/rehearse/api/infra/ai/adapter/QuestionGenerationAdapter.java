package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.*;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QuestionGenerationAdapter {

    private static final int MAX_TOKENS = 8192;
    private static final double TEMPERATURE = 0.9;

    private final QuestionGenerationPromptBuilder promptBuilder;
    private final AiResponseParser responseParser;

    public List<GeneratedQuestion> adapt(AiClient client, QuestionGenerationRequest req) {
        String systemPrompt = promptBuilder.buildSystemPrompt(req);
        String userPrompt = promptBuilder.buildUserPrompt(req);

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.ofCached(ChatMessage.Role.SYSTEM, systemPrompt),
                        ChatMessage.of(ChatMessage.Role.USER, userPrompt)
                ))
                .maxTokens(MAX_TOKENS)
                .temperature(TEMPERATURE)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .callType("generate_questions")
                .build();

        ChatResponse response = client.chat(chatRequest);
        GeneratedQuestionsWrapper wrapper = responseParser.parseOrRetry(
                response, GeneratedQuestionsWrapper.class, client, chatRequest);

        if (wrapper.getQuestions() == null || wrapper.getQuestions().isEmpty()) {
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }
        return wrapper.getQuestions();
    }
}
