package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.interview.models.service.AnswerAnalyzer;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.OpenAiAnswerAnalyzerClient;
import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;
import com.rehearse.api.infra.ai.prompt.AnswerAnalysisPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiAnswerAnalyzer implements AnswerAnalyzer {

    private final OpenAiAnswerAnalyzerClient client;
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
        String content = client.call(prompt.system(), prompt.user());
        return aiResponseParser.parseJsonResponse(content, GeneratedAnswerAnalysis.class);
    }
}
