package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.*;
import com.rehearse.api.infra.ai.dto.openai.OpenAiRequest;
import com.rehearse.api.infra.ai.dto.openai.OpenAiResponse;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import com.rehearse.api.infra.ai.prompt.FollowUpPromptBuilder;
import com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiClient {

    private static final int MAX_TOKENS_QUESTION = 8192;
    private static final int MAX_TOKENS_FOLLOW_UP = 1024;
    private static final double TEMPERATURE_FOLLOW_UP = 0.7;

    private static final int MAX_RETRY_ATTEMPTS = 2;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;

    private static final long MAX_AUDIO_BYTES = 10L * 1024 * 1024;

    private final RestClient restClient;
    private final QuestionGenerationPromptBuilder questionPromptBuilder;
    private final FollowUpPromptBuilder followUpPromptBuilder;
    private final AiResponseParser responseParser;
    private final String apiKey;
    private final String model;
    private final String audioModel;

    public OpenAiClient(
            RestClient.Builder restClientBuilder,
            QuestionGenerationPromptBuilder questionPromptBuilder,
            FollowUpPromptBuilder followUpPromptBuilder,
            AiResponseParser responseParser,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model,
            @Value("${openai.audio-model:gpt-4o-mini-audio-preview}") String audioModel,
            @Value("${openai.base-url:https://api.openai.com/v1/chat/completions}") String baseUrl) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(60));

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
        this.questionPromptBuilder = questionPromptBuilder;
        this.followUpPromptBuilder = followUpPromptBuilder;
        this.responseParser = responseParser;
        this.apiKey = apiKey;
        this.model = model;
        this.audioModel = audioModel;
    }

    @RateLimiter(name = "openai-api")
    public ChatResponse chat(ChatRequest req) {
        String resolvedModel = resolveModel(req, model);
        int resolvedMaxTokens = (req.maxTokens() != null) ? req.maxTokens() : MAX_TOKENS_FOLLOW_UP;

        List<Map<String, Object>> messages = toMessages(req.messages());

        Map<String, Object> requestBody = buildRequestBody(resolvedModel, messages, resolvedMaxTokens, req);

        String apiLabel = "OpenAI API [" + req.callType() + "]";
        OpenAiResponse openAiResponse = executeWithRetry(requestBody, apiLabel, resolvedMaxTokens);
        return toChatResponse(openAiResponse, resolvedModel);
    }

    @RateLimiter(name = "openai-api")
    public ChatResponse chatWithAudio(ChatRequest req, MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(AiErrorCode.CLIENT_ERROR);
        }
        if (audioFile.getSize() > MAX_AUDIO_BYTES) {
            log.warn("[OpenAI Audio Chat] 파일 크기 초과: size={} bytes, max={}", audioFile.getSize(), MAX_AUDIO_BYTES);
            throw new BusinessException(AiErrorCode.CLIENT_ERROR);
        }

        String resolvedModel = resolveModel(req, audioModel);
        int resolvedMaxTokens = (req.maxTokens() != null) ? req.maxTokens() : MAX_TOKENS_FOLLOW_UP;

        String audioBase64 = encodeAudioToBase64(audioFile);
        String audioFormat = resolveAudioFormat(audioFile.getOriginalFilename());

        StringBuilder userText = new StringBuilder();
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessage msg : req.messages()) {
            if (msg.role() == ChatMessage.Role.USER) {
                if (userText.length() > 0) {
                    userText.append("\n");
                }
                userText.append(msg.content());
            } else {
                Map<String, Object> entry = new HashMap<>();
                entry.put("role", msg.roleLowercase());
                entry.put("content", msg.content());
                messages.add(entry);
            }
        }
        messages.add(buildAudioUserContent(userText.toString(), audioBase64, audioFormat));

        Map<String, Object> requestBody = buildAudioRequestBody(resolvedModel, messages, resolvedMaxTokens, req);

        String apiLabel = "OpenAI Audio Chat [" + req.callType() + "]";
        OpenAiResponse openAiResponse = executeWithRetry(requestBody, apiLabel, resolvedMaxTokens);
        return toChatResponse(openAiResponse, resolvedModel);
    }

    @RateLimiter(name = "openai-api")
    public List<GeneratedQuestion> generateQuestions(QuestionGenerationRequest request) {
        String systemPrompt = questionPromptBuilder.buildSystemPrompt(request);
        String userPrompt = questionPromptBuilder.buildUserPrompt(request);

        String text = callOpenAiApi(systemPrompt, userPrompt, MAX_TOKENS_QUESTION, 0.9);
        GeneratedQuestionsWrapper wrapper = responseParser.parseJsonResponse(text, GeneratedQuestionsWrapper.class);

        if (wrapper.getQuestions() == null || wrapper.getQuestions().isEmpty()) {
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }

        return wrapper.getQuestions();
    }

    @RateLimiter(name = "openai-api")
    public GeneratedFollowUp generateFollowUpQuestion(FollowUpGenerationRequest request) {
        String systemPrompt = followUpPromptBuilder.buildSystemPrompt(request);
        String userPrompt = followUpPromptBuilder.buildUserPrompt(request);

        String text = callOpenAiApi(systemPrompt, userPrompt, MAX_TOKENS_FOLLOW_UP, TEMPERATURE_FOLLOW_UP);
        return responseParser.parseJsonResponse(text, GeneratedFollowUp.class);
    }

    @RateLimiter(name = "openai-api")
    public GeneratedFollowUp generateFollowUpWithAudio(MultipartFile audioFile, FollowUpGenerationRequest request) {
        String systemPrompt = followUpPromptBuilder.buildSystemPrompt(request);
        String userPrompt = followUpPromptBuilder.buildUserPromptForAudio(request);

        String audioBase64 = encodeAudioToBase64(audioFile);
        String audioFormat = resolveAudioFormat(audioFile.getOriginalFilename());

        String text = callOpenAiAudioApi(systemPrompt, userPrompt, audioBase64, audioFormat);
        return responseParser.parseJsonResponse(text, GeneratedFollowUp.class);
    }

    // --- private helpers ---

    private String resolveModel(ChatRequest req, String defaultModel) {
        return (req.modelOverride() != null && !req.modelOverride().isBlank())
                ? req.modelOverride()
                : defaultModel;
    }

    private List<Map<String, Object>> toMessages(List<ChatMessage> chatMessages) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessage msg : chatMessages) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("role", msg.roleLowercase());
            entry.put("content", msg.content());
            messages.add(entry);
        }
        return messages;
    }

    private String encodeAudioToBase64(MultipartFile audioFile) {
        if (audioFile.getSize() > MAX_AUDIO_BYTES) {
            log.warn("[OpenAI] 오디오 파일 크기 초과: size={} bytes, max={}", audioFile.getSize(), MAX_AUDIO_BYTES);
            throw new BusinessException(AiErrorCode.CLIENT_ERROR);
        }
        try {
            return Base64.getEncoder().encodeToString(audioFile.getBytes());
        } catch (IOException e) {
            log.error("[OpenAI] 오디오 파일 읽기 실패: {}", e.getMessage());
            throw new BusinessException(AiErrorCode.CLIENT_ERROR);
        }
    }

    private Map<String, Object> buildAudioUserContent(String userText, String audioBase64, String audioFormat) {
        return Map.of("role", "user", "content", List.of(
                Map.of("type", "text", "text", userText),
                Map.of("type", "input_audio", "input_audio",
                        Map.of("data", audioBase64, "format", audioFormat))
        ));
    }

    static Map<String, Object> buildRequestBody(
            String resolvedModel, List<Map<String, Object>> messages, int maxTokens, ChatRequest req) {
        Map<String, Object> body = baseRequestBody(resolvedModel, messages, maxTokens, req);
        if (req.responseFormat() == ResponseFormat.JSON_OBJECT) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return body;
    }

    // gpt-4o-*-audio-preview 는 response_format=json_object 파라미터를 거부한다 (400 BAD_REQUEST).
    // JSON 강제는 system prompt 의 출력 규칙으로 보장한다.
    static Map<String, Object> buildAudioRequestBody(
            String resolvedModel, List<Map<String, Object>> messages, int maxTokens, ChatRequest req) {
        return baseRequestBody(resolvedModel, messages, maxTokens, req);
    }

    private static Map<String, Object> baseRequestBody(
            String resolvedModel, List<Map<String, Object>> messages, int maxTokens, ChatRequest req) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", resolvedModel);
        body.put("messages", messages);
        body.put("max_tokens", maxTokens);
        if (req.temperature() != null) {
            body.put("temperature", req.temperature());
        }
        return body;
    }

    private ChatResponse toChatResponse(OpenAiResponse openAiResponse, String resolvedModel) {
        int inputTokens = 0;
        int outputTokens = 0;
        int cachedTokens = 0;
        if (openAiResponse.getUsage() != null) {
            inputTokens = openAiResponse.getUsage().getPromptTokens();
            outputTokens = openAiResponse.getUsage().getCompletionTokens();
            cachedTokens = openAiResponse.getUsage().getCachedTokens();
        }
        boolean cacheHit = cachedTokens > 0;
        String content = openAiResponse.getChoices().get(0).getMessage().getContent();
        return new ChatResponse(
                content,
                ChatResponse.Usage.of(inputTokens, outputTokens, cachedTokens, 0),
                "openai",
                resolvedModel,
                cacheHit,
                false
        );
    }

    private String resolveAudioFormat(String filename) {
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            return switch (ext) {
                case "webm" -> "wav";
                case "wav" -> "wav";
                case "mp3" -> "mp3";
                case "ogg" -> "wav";
                default -> "wav";
            };
        }
        return "wav";
    }

    private String callOpenAiApi(String systemPrompt, String userPrompt, int maxTokens, Double temperature) {
        Object requestBody = OpenAiRequest.builder()
                .model(model)
                .messages(List.of(
                        OpenAiRequest.Message.builder()
                                .role("system")
                                .content(systemPrompt)
                                .build(),
                        OpenAiRequest.Message.builder()
                                .role("user")
                                .content(userPrompt)
                                .build()
                ))
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();

        return executeWithRetry(requestBody, "OpenAI API", maxTokens)
                .getChoices().get(0).getMessage().getContent();
    }

    private String callOpenAiAudioApi(String systemPrompt, String userPrompt, String audioBase64, String audioFormat) {
        Object requestBody = Map.of(
                "model", audioModel,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        buildAudioUserContent(userPrompt, audioBase64, audioFormat)
                ),
                "max_tokens", MAX_TOKENS_FOLLOW_UP,
                "temperature", TEMPERATURE_FOLLOW_UP
        );

        return executeWithRetry(requestBody, "OpenAI Audio API", MAX_TOKENS_FOLLOW_UP)
                .getChoices().get(0).getMessage().getContent();
    }

    private OpenAiResponse executeWithRetry(Object requestBody, String apiLabel, int maxTokens) {
        long delayMs = INITIAL_RETRY_DELAY_MS;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
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
                            String body = res.getBody() != null ? new String(res.getBody().readAllBytes()) : "(empty body)";
                            log.error("[{}] 클라이언트 에러: status={}, body={}", apiLabel, res.getStatusCode(), body);
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
                    log.info("[{}] 토큰 사용량 - prompt: {}, completion: {}, total: {}",
                            apiLabel, usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
                }

                OpenAiResponse.Choice choice = response.getChoices().get(0);
                if ("length".equals(choice.getFinishReason())) {
                    log.warn("[{}] 응답이 max_tokens({})에 도달하여 잘림", apiLabel, maxTokens);
                }

                String content = choice.getMessage() != null ? choice.getMessage().getContent() : null;
                if (content == null || content.isBlank()) {
                    throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
                }
                return response;

            } catch (BusinessException e) {
                throw e;
            } catch (RetryableApiException | RestClientException e) {
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.info("[{}] 재시도 {}/{}: {}", apiLabel, attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException(AiErrorCode.SERVER_ERROR);
                    }
                    delayMs *= 2;
                } else {
                    log.error("[{}] 모든 재시도 실패", apiLabel, e);
                    throw new BusinessException(AiErrorCode.TIMEOUT);
                }
            }
        }
        throw new BusinessException(AiErrorCode.TIMEOUT);
    }
}
