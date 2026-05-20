package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.question.models.service.ResumeQuestionGenerator;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.OpenAiResumeQuestionClient;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;
import com.rehearse.api.infra.ai.prompt.ResumeQuestionPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiResumeQuestionGenerator implements ResumeQuestionGenerator {

    private final OpenAiResumeQuestionClient client;
    private final ResumeQuestionPromptBuilder promptBuilder;
    private final AiResponseParser aiResponseParser;

    @Override
    public GeneratedResumeQuestions generate(ResumeSkeleton skeleton, int openerCount, int mainCount) {
        ResumeQuestionPromptBuilder.PromptPair prompt = promptBuilder.build(skeleton, openerCount, mainCount);
        String content = client.call(prompt.system(), prompt.user());
        return aiResponseParser.parseJsonResponse(content, GeneratedResumeQuestions.class);
    }
}
