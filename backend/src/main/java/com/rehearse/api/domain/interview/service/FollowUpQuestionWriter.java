package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.vo.AskedPerspectives;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.AnswerAnalysisJsonRenderer;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpQuestionWriter {

    private static final String CALL_TYPE = "follow_up_generator_v3";
    private static final double TEMPERATURE = 0.6;
    private static final int MAX_TOKENS = 1024;

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final InterviewContextBuilder contextBuilder;

    public GeneratedFollowUp write(
            FollowUpGenerationRequest req,
            AnswerAnalysis analysis,
            AskedPerspectives askedPerspectives
    ) {
        String answerAnalysisJson = AnswerAnalysisJsonRenderer.render(analysis, askedPerspectives.values());
        BuiltContext built = buildContext(req, answerAnalysisJson, askedPerspectives);
        ChatRequest chatRequest = buildChatRequest(built);

        ChatResponse response = aiClient.chat(chatRequest);
        GeneratedFollowUp parsed = aiResponseParser.parseOrRetry(
                response, GeneratedFollowUp.class, aiClient, chatRequest);
        return parsed.withAnswerText(req.answerText());
    }

    private BuiltContext buildContext(
            FollowUpGenerationRequest req,
            String answerAnalysisJson,
            AskedPerspectives askedPerspectives
    ) {
        return contextBuilder.build(new ContextBuildRequest(
                CALL_TYPE,
                Map.of(),
                req.previousExchanges() != null ? req.previousExchanges() : List.of(),
                Map.of(
                        "answerAnalysisJson", answerAnalysisJson,
                        "askedPerspectives", askedPerspectives.values().stream()
                                .map(Enum::name)
                                .collect(Collectors.joining(", "))
                ),
                null
        ));
    }

    private static ChatRequest buildChatRequest(BuiltContext built) {
        return ChatRequest.builder()
                .messages(built.messages())
                .callType(CALL_TYPE)
                .temperature(TEMPERATURE)
                .maxTokens(MAX_TOKENS)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .build();
    }
}
