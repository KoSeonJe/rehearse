package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.interview.models.service.AudioTurnAnalyzer;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.OpenAiAudioTurnAnalyzerClient;
import com.rehearse.api.infra.ai.dto.GeneratedTurnAnalysis;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.AudioChatFallbackRequiredException;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import com.rehearse.api.infra.ai.prompt.AudioTurnAnalyzerPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiAudioTurnAnalyzer implements AudioTurnAnalyzer {

    private final OpenAiAudioTurnAnalyzerClient client;
    private final AudioTurnAnalyzerPromptBuilder promptBuilder;
    private final AiResponseParser aiResponseParser;

    // PARSE_FAILED 는 응답 구조 결함 → text-only fallback 도 동일 위험 → rethrow.
    // 그 외 BusinessException (CLIENT_ERROR / TIMEOUT 등) 과 네트워크 오류는 fallback 신호로 변환.
    @Override
    public GeneratedTurnAnalysis analyze(
            MultipartFile audio,
            String mainQuestion,
            ReferenceType questionReferenceType
    ) throws AudioChatFallbackRequiredException {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPromptText(mainQuestion, questionReferenceType);

        try {
            String content = client.call(systemPrompt, userPrompt, audio);
            return aiResponseParser.parseJsonResponse(content, GeneratedTurnAnalysis.class);
        } catch (BusinessException e) {
            if (isResponseDefect(e)) {
                throw e;
            }
            log.warn("[OpenAiAudioTurnAnalyzer] audio chat 실패 → fallback 변환: code={}", e.getCode());
            throw new AudioChatFallbackRequiredException("audio chat 인프라 오류: " + e.getMessage(), e);
        } catch (RestClientException | RetryableApiException e) {
            log.warn("[OpenAiAudioTurnAnalyzer] audio chat 네트워크 오류 → fallback: {}", e.getMessage());
            throw new AudioChatFallbackRequiredException("audio chat 네트워크 오류: " + e.getMessage(), e);
        }
    }

    private boolean isResponseDefect(BusinessException e) {
        return AiErrorCode.PARSE_FAILED.getCode().equals(e.getCode());
    }
}
