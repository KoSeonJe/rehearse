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

/**
 * QuestionGenerationRequest → ChatRequest 변환 후 AiClient.chat() 경유로 질문 생성.
 *
 * <p>AiClient 구현체(ResilientAiClient, MockAiClient)가 이 어댑터를 주입받아
 * generateQuestions() 를 chat() 경유로 위임한다.</p>
 */
@Component
@RequiredArgsConstructor
public class QuestionGenerationAdapter {

    private static final int MAX_TOKENS = 8192;
    private static final double TEMPERATURE = 0.9;

    private final QuestionGenerationPromptBuilder promptBuilder;
    private final AiResponseParser responseParser;

    /**
     * QuestionGenerationRequest 를 ChatRequest 로 변환하고 client.chat() 을 호출해
     * GeneratedQuestion 목록을 반환한다.
     */
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
