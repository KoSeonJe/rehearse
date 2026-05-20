package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.FocusHints;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import com.rehearse.api.infra.ai.prompt.PromptFormatters;
import com.rehearse.api.infra.ai.schema.GeneratedAnswerAnalysisSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerAnalyzer {

    private static final String CALL_TYPE = "answer_analyzer";
    private static final double TEMPERATURE = 0.2;
    private static final int MAX_TOKENS = 800;

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final InterviewContextBuilder contextBuilder;

    public AnswerAnalysis analyze(
            Long interviewId,
            String mainQuestion,
            ReferenceType questionReferenceType,
            String userAnswer,
            boolean isResumeTrack
    ) {
        if (interviewId == null) {
            throw new IllegalArgumentException("interviewId 는 null 일 수 없습니다.");
        }

        String personaDepthHint = PromptFormatters.toReferenceLabel(questionReferenceType);

        BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                CALL_TYPE,
                new FocusHints.AnswerAnalyzerHints(
                        mainQuestion != null ? mainQuestion : "",
                        userAnswer != null ? userAnswer : "",
                        personaDepthHint,
                        isResumeTrack
                ),
                null,
                null,
                null
        ));

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(built.messages())
                .callType(CALL_TYPE)
                .temperature(TEMPERATURE)
                .maxTokens(MAX_TOKENS)
                .responseFormat(ResponseFormat.JSON_SCHEMA)
                .jsonSchema(GeneratedAnswerAnalysisSchema.spec(isResumeTrack))
                .build();

        ChatResponse response = aiClient.chat(chatRequest);
        GeneratedAnswerAnalysis parsed = aiResponseParser.parseOrRetry(
                response, GeneratedAnswerAnalysis.class, aiClient, chatRequest);

        return parsed.toDomain();
    }
}
