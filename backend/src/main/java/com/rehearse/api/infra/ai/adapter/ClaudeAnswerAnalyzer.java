package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.interview.models.service.AnswerAnalyzer;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.ClaudeAnswerAnalyzerClient;
import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;
import com.rehearse.api.infra.ai.prompt.AnswerAnalysisPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${claude.api-key:}'.isEmpty()")
public class ClaudeAnswerAnalyzer implements AnswerAnalyzer {

    private static final String JSON_OBJECT_INSTRUCTION =
            "You MUST respond with a single JSON object only. No prose, no markdown, no code fences.";

    private final ClaudeAnswerAnalyzerClient client;
    private final AnswerAnalysisPromptBuilder promptBuilder;
    private final AiResponseParser aiResponseParser;

    @Override
    public GeneratedAnswerAnalysis analyze(
            Long interviewId,
            String mainQuestion,
            ReferenceType questionReferenceType,
            String userAnswer,
            boolean isResumeTrack
    ) {
        AnswerAnalysisPromptBuilder.PromptPair prompt = promptBuilder.build(mainQuestion, questionReferenceType, userAnswer, isResumeTrack);
        String systemPrompt = JSON_OBJECT_INSTRUCTION + "\n\n" + prompt.system();
        String content = client.call(systemPrompt, prompt.user());
        return aiResponseParser.parseJsonResponse(content, GeneratedAnswerAnalysis.class);
    }
}
