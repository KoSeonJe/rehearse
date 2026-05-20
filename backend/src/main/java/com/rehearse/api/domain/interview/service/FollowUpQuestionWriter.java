package com.rehearse.api.domain.interview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.FocusHints;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final ObjectMapper objectMapper;

    public GeneratedFollowUp write(
            String mainQuestion,
            String userAnswer,
            AnswerAnalysis analysis,
            ResumeSkeleton resumeSkeleton
    ) {
        BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                CALL_TYPE,
                new FocusHints.FollowUpGeneratorV3Hints(
                        mainQuestion != null ? mainQuestion : "",
                        userAnswer != null ? userAnswer : "",
                        analysis.claims().stream().map(c -> c.text()).toList(),
                        analysis.dimensionGaps(),
                        analysis.weakestDimension(),
                        analysis.unstatedAssumptions(),
                        serializeSkeleton(resumeSkeleton)
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
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .build();

        ChatResponse response = aiClient.chat(chatRequest);
        GeneratedFollowUp parsed = aiResponseParser.parseOrRetry(
                response, GeneratedFollowUp.class, aiClient, chatRequest);
        return parsed.withAnswerText(userAnswer);
    }

    private String serializeSkeleton(ResumeSkeleton skeleton) {
        if (skeleton == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(skeleton.projects());
        } catch (JsonProcessingException e) {
            log.warn("[FollowUpQuestionWriter] skeleton 직렬화 실패 — 빈 문자열 사용", e);
            return "";
        }
    }
}
