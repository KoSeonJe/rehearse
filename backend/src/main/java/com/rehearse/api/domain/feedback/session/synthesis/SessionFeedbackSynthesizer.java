package com.rehearse.api.domain.feedback.session.synthesis;

import com.rehearse.api.domain.feedback.session.dto.SessionFeedbackPayload;
import com.rehearse.api.domain.feedback.session.exception.SessionFeedbackParseException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.prompt.SessionFeedbackSynthesizerPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionFeedbackSynthesizer {

    private final AiClient aiClient;
    private final SessionFeedbackSynthesizerPromptBuilder promptBuilder;
    private final SessionFeedbackParser parser;

    public SessionFeedbackPayload synthesize(SessionFeedbackInput input) {
        var request = promptBuilder.build(input);
        ChatResponse response = aiClient.chat(request);
        String json = response.content();

        try {
            return parser.parse(json, input);
        } catch (SessionFeedbackParseException e) {
            log.warn("파싱 실패 — 1회 재시도: reason={}", e.getMessage());
            ChatResponse retryResponse = aiClient.chat(request);
            return parser.parse(retryResponse.content(), input);
        }
    }
}
