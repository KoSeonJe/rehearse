package com.rehearse.api.infra.ai.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.models.service.ResumeSkeletonExtractor;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiResumeSkeletonExtractor implements ResumeSkeletonExtractor {

    private static final String CALL_TYPE = "resume_skeleton_extractor";
    private static final String SYSTEM_PROMPT_PATH = "prompts/template/resume/resume-extractor.txt";
    private static final String PDF_FILENAME = "resume.pdf";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    private String systemPrompt;

    public OpenAiResumeSkeletonExtractor(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${openai.api-key}") String apiKey,
            @Value("${ai.resume.skeleton.model:gpt-4o-mini}") String model,
            @Value("${ai.resume.skeleton.timeout-ms:60000}") long timeoutMs,
            @Value("${ai.resume.skeleton.max-tokens:12000}") int maxTokens,
            @Value("${ai.resume.skeleton.temperature:0.2}") double temperature,
            @Value("${ai.resume.skeleton.base-url:https://api.openai.com/v1/responses}") String baseUrl) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    @PostConstruct
    void init() {
        try {
            systemPrompt = StreamUtils.copyToString(
                    new ClassPathResource(SYSTEM_PROMPT_PATH).getInputStream(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("resume-extractor 프롬프트 로드 실패", e);
        }
    }

    @Override
    public GeneratedResumeSkeleton extract(byte[] pdfBytes, String fileHash) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new BusinessException(AiErrorCode.CLIENT_ERROR);
        }

        String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
        Map<String, Object> requestBody = buildRequestBody(base64Pdf);

        String responseBody = executeOnce(requestBody);
        String outputText = extractOutputText(responseBody);
        return parseSkeleton(outputText, fileHash);
    }

    private Map<String, Object> buildRequestBody(String base64Pdf) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_output_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("input", List.of(
                Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "input_text", "text", systemPrompt),
                                Map.of(
                                        "type", "input_file",
                                        "filename", PDF_FILENAME,
                                        "file_data", "data:application/pdf;base64," + base64Pdf
                                )
                        )
                )
        ));
        return body;
    }

    private String executeOnce(Map<String, Object> requestBody) {
        String apiLabel = "OpenAI Responses [" + CALL_TYPE + "]";
        String responseBody = restClient.post()
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
                    throw new BusinessException(AiErrorCode.SERVER_ERROR);
                })
                .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            log.error("[{}] 응답 본문 비어있음", apiLabel);
            throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
        }
        return responseBody;
    }

    private String extractOutputText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode output = root.path("output");
            if (!output.isArray() || output.isEmpty()) {
                log.error("[OpenAI Responses] output 배열 누락: bodyLen={}, bodyPreview={}",
                        responseBody.length(), maskPreview(responseBody));
                throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
            }
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode part : content) {
                    String type = part.path("type").asText("");
                    if ("output_text".equals(type)) {
                        String text = part.path("text").asText("");
                        if (!text.isBlank()) {
                            return text;
                        }
                    }
                }
            }
            log.error("[OpenAI Responses] output_text 추출 실패: bodyLen={}, bodyPreview={}",
                    responseBody.length(), maskPreview(responseBody));
            throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
        } catch (JsonProcessingException e) {
            log.error("[OpenAI Responses] 응답 JSON 파싱 실패", e);
            throw new BusinessException(AiErrorCode.RESPONSE_INVALID);
        }
    }

    private GeneratedResumeSkeleton parseSkeleton(String outputText, String fileHash) {
        String json = stripJsonCodeFence(outputText);
        try {
            return objectMapper.readValue(json, GeneratedResumeSkeleton.class);
        } catch (JsonProcessingException e) {
            log.error("[OpenAI Responses] skeleton JSON 파싱 실패 fileHash={}, textLen={}, textPreview={}",
                    maskHash(fileHash), outputText.length(), maskPreview(outputText), e);
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }
    }

    private String maskHash(String fileHash) {
        if (fileHash == null || fileHash.length() < 8) {
            return "(none)";
        }
        return fileHash.substring(0, 8);
    }

    private String maskPreview(String text) {
        if (text == null) {
            return "(null)";
        }
        if (text.length() <= 80) {
            return text;
        }
        return text.substring(0, 80) + "...";
    }

    private String stripJsonCodeFence(String text) {
        String json = text.trim();
        if (json.startsWith("```json")) {
            json = json.substring(7);
            int end = json.lastIndexOf("```");
            if (end >= 0) {
                json = json.substring(0, end);
            }
        } else if (json.startsWith("```")) {
            json = json.substring(3);
            int end = json.lastIndexOf("```");
            if (end >= 0) {
                json = json.substring(0, end);
            }
        }
        return json.trim();
    }
}
