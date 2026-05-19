package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.resume.models.service.ResumeSkeletonExtractor;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.OpenAiResponsesOutputTextExtractor;
import com.rehearse.api.infra.ai.config.OpenAiCommonProperties;
import com.rehearse.api.infra.ai.config.OpenAiResumeSkeletonProperties;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private final OpenAiResponsesOutputTextExtractor outputTextExtractor;
    private final AiResponseParser aiResponseParser;
    private final String apiKey;
    private final OpenAiResumeSkeletonProperties properties;

    private String systemPrompt;

    public OpenAiResumeSkeletonExtractor(
            @Qualifier("openAiResumeExtractorRestClient") RestClient openAiResumeExtractorRestClient,
            OpenAiResponsesOutputTextExtractor outputTextExtractor,
            AiResponseParser aiResponseParser,
            OpenAiResumeSkeletonProperties properties,
            OpenAiCommonProperties commonProperties) {
        this.restClient = openAiResumeExtractorRestClient;
        this.outputTextExtractor = outputTextExtractor;
        this.aiResponseParser = aiResponseParser;
        this.properties = properties;
        this.apiKey = commonProperties.apiKey();
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
        String outputText = outputTextExtractor.extract(responseBody);
        return aiResponseParser.parseJsonResponse(outputText, GeneratedResumeSkeleton.class);
    }

    private Map<String, Object> buildRequestBody(String base64Pdf) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.model());
        body.put("max_output_tokens", properties.maxTokens());
        body.put("temperature", properties.temperature());
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
}
