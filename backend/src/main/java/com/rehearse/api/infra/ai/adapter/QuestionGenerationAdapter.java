package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.*;
import com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class QuestionGenerationAdapter {

    private static final int MAX_TOKENS = 8192;
    private static final double TEMPERATURE = 0.9;
    static final String SCHEMA_NAME = "generated_questions";

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
                .responseFormat(ResponseFormat.JSON_SCHEMA)
                .jsonSchema(new JsonSchemaSpec(SCHEMA_NAME, buildJsonSchema()))
                .callType("generate_questions")
                .build();

        ChatResponse response = client.chat(chatRequest);
        GeneratedQuestionsWrapper wrapper = responseParser.parseOrRetry(
                response, GeneratedQuestionsWrapper.class, client, chatRequest);

        return wrapper.questions();
    }

    static Map<String, Object> buildJsonSchema() {
        Map<String, Object> questionProperties = new LinkedHashMap<>();
        questionProperties.put("content", Map.of("type", "string"));
        questionProperties.put("tts_content", Map.of("type", "string"));
        questionProperties.put("category", Map.of("type", "string"));
        questionProperties.put("order", Map.of("type", "integer"));
        questionProperties.put("evaluation_criteria", Map.of("type", "string"));
        questionProperties.put("question_category", Map.of("type", "string"));
        questionProperties.put("best_answer", Map.of("type", "string"));

        Map<String, Object> questionSchema = new LinkedHashMap<>();
        questionSchema.put("type", "object");
        questionSchema.put("additionalProperties", false);
        questionSchema.put("required", List.of(
                "content", "tts_content", "category", "order",
                "evaluation_criteria", "question_category", "best_answer"));
        questionSchema.put("properties", questionProperties);

        Map<String, Object> questionsArray = new LinkedHashMap<>();
        questionsArray.put("type", "array");
        questionsArray.put("items", questionSchema);

        Map<String, Object> rootProperties = new LinkedHashMap<>();
        rootProperties.put("questions", questionsArray);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("questions"));
        schema.put("properties", rootProperties);
        return schema;
    }
}
