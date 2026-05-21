package com.rehearse.api.infra.ai.client;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.properties.OpenAiAudioTurnAnalyzerProperties;
import com.rehearse.api.infra.ai.properties.OpenAiCommonProperties;
import com.rehearse.api.infra.ai.dto.openai.OpenAiResponse;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiAudioTurnAnalyzerClient {

    private static final String CALL_TYPE = "audio_turn_analyzer";
    private static final long MAX_AUDIO_BYTES = 10L * 1024 * 1024;

    private final RestClient restClient;
    private final OpenAiAudioTurnAnalyzerProperties properties;
    private final String apiKey;

    public OpenAiAudioTurnAnalyzerClient(
            @Qualifier("openAiAudioTurnAnalyzerRestClient") RestClient openAiAudioTurnAnalyzerRestClient,
            OpenAiAudioTurnAnalyzerProperties properties,
            OpenAiCommonProperties commonProperties) {
        this.restClient = openAiAudioTurnAnalyzerRestClient;
        this.properties = properties;
        this.apiKey = commonProperties.apiKey();
    }

    @RateLimiter(name = "openai-api")
    @Retryable(
            retryFor = {RetryableApiException.class, RestClientException.class},
            noRetryFor = {BusinessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, random = true)
    )
    public String call(String systemPrompt, String userPrompt, MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(AiErrorCode.CLIENT_ERROR);
        }
        if (audioFile.getSize() > MAX_AUDIO_BYTES) {
            log.warn("[OpenAI Audio Turn Analyzer] 파일 크기 초과: size={} bytes, max={}",
                    audioFile.getSize(), MAX_AUDIO_BYTES);
            throw new BusinessException(AiErrorCode.CLIENT_ERROR);
        }

        String audioBase64 = encodeAudioToBase64(audioFile);
        String audioFormat = resolveAudioFormat(audioFile.getOriginalFilename());

        Map<String, Object> body = buildRequestBody(systemPrompt, userPrompt, audioBase64, audioFormat);
        return execute(body);
    }

    @Recover
    public String recoverCall(Exception e, String systemPrompt, String userPrompt, MultipartFile audioFile) {
        log.error("[OpenAI Audio Turn Analyzer] 재시도 최종 실패 callType={}", CALL_TYPE, e);
        throw new BusinessException(AiErrorCode.TIMEOUT);
    }

    // gpt-4o-*-audio-preview 는 response_format=json_object 파라미터를 거부 (400). system prompt 로 JSON 강제.
    private Map<String, Object> buildRequestBody(
            String systemPrompt, String userPrompt, String audioBase64, String audioFormat) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.model());
        body.put("max_tokens", properties.maxTokens());
        body.put("temperature", properties.temperature());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", List.of(
                        Map.of("type", "text", "text", userPrompt),
                        Map.of("type", "input_audio", "input_audio",
                                Map.of("data", audioBase64, "format", audioFormat))
                ))
        ));
        return body;
    }

    private String encodeAudioToBase64(MultipartFile audioFile) {
        try {
            return Base64.getEncoder().encodeToString(audioFile.getBytes());
        } catch (IOException e) {
            log.error("[OpenAI Audio Turn Analyzer] 오디오 파일 읽기 실패: {}", e.getMessage());
            throw new BusinessException(AiErrorCode.CLIENT_ERROR);
        }
    }

    private String resolveAudioFormat(String filename) {
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            return switch (ext) {
                case "wav" -> "wav";
                case "mp3" -> "mp3";
                case "webm", "ogg" -> "wav";
                default -> "wav";
            };
        }
        return "wav";
    }

    private String execute(Map<String, Object> requestBody) {
        String apiLabel = "OpenAI Audio Chat [" + CALL_TYPE + "]";
        OpenAiResponse response = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(requestBody)
                .retrieve()
                .onStatus(status -> status.value() == 429, (req, res) -> {
                    log.warn("[{}] Rate Limited (429)", apiLabel);
                    throw new RetryableApiException(apiLabel + " rate limited (429)");
                })
                .onStatus(status -> status.is4xxClientError() && status.value() != 429, (req, res) -> {
                    String errBody = res.getBody() != null ? new String(res.getBody().readAllBytes()) : "(empty body)";
                    log.error("[{}] 클라이언트 에러: status={}, body={}", apiLabel, res.getStatusCode(), errBody);
                    throw new BusinessException(AiErrorCode.CLIENT_ERROR);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    log.warn("[{}] 서버 에러: status={}", apiLabel, res.getStatusCode());
                    throw new RetryableApiException(apiLabel + " 서버 에러: " + res.getStatusCode());
                })
                .body(OpenAiResponse.class);

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
        }

        if (response.getUsage() != null) {
            var usage = response.getUsage();
            log.info("[{}] 토큰 사용량 prompt={}, completion={}, total={}",
                    apiLabel, usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
        }

        OpenAiResponse.Choice choice = response.getChoices().get(0);
        String content = choice.getMessage() != null ? choice.getMessage().getContent() : null;
        if (content == null || content.isBlank()) {
            throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
        }
        return content;
    }
}
