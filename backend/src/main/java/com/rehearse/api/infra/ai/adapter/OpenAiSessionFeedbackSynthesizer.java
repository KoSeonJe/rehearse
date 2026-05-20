package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.feedback.session.exception.SessionFeedbackParseException;
import com.rehearse.api.domain.feedback.session.models.service.SessionFeedbackSynthesizer;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackParser;
import com.rehearse.api.infra.ai.client.OpenAiSessionFeedbackSynthesizerClient;
import com.rehearse.api.infra.ai.dto.GeneratedSessionFeedback;
import com.rehearse.api.infra.ai.prompt.SessionFeedbackSynthesizerPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiSessionFeedbackSynthesizer implements SessionFeedbackSynthesizer {

    private final OpenAiSessionFeedbackSynthesizerClient client;
    private final SessionFeedbackSynthesizerPromptBuilder promptBuilder;
    private final SessionFeedbackParser parser;

    @Override
    public GeneratedSessionFeedback synthesize(SessionFeedbackInput input) {
        SessionFeedbackSynthesizerPromptBuilder.PromptPair prompt = promptBuilder.build(input);
        String content = client.call(prompt.system(), prompt.user());

        try {
            return parser.parse(content, input);
        } catch (SessionFeedbackParseException e) {
            log.warn("[OpenAI Session Feedback] 파싱 실패 — 1회 재시도: reason={}", e.getMessage());
            String retryContent = client.call(prompt.system(), prompt.user());
            return parser.parse(retryContent, input);
        }
    }
}
