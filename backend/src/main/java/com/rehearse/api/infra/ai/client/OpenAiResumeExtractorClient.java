package com.rehearse.api.infra.ai.client;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.config.OpenAiCommonProperties;
import com.rehearse.api.infra.ai.config.OpenAiResumeSkeletonProperties;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiResumeExtractorClient {

    private static final String CALL_TYPE = "resume_skeleton_extractor";
    private static final String PDF_FILENAME = "resume.pdf";

    private final RestClient restClient;
    private final OpenAiResumeSkeletonProperties properties;
    private final String apiKey;

    public OpenAiResumeExtractorClient(
            @Qualifier("openAiResumeExtractorRestClient") RestClient openAiResumeExtractorRestClient,
            OpenAiResumeSkeletonProperties properties,
            OpenAiCommonProperties commonProperties) {
        this.restClient = openAiResumeExtractorRestClient;
        this.properties = properties;
        this.apiKey = commonProperties.apiKey();
    }

    public String call(String systemPrompt, String base64Pdf) {
        Map<String, Object> requestBody = buildRequestBody(systemPrompt, base64Pdf);
        return execute(requestBody);
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, String base64Pdf) {
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

    private String execute(Map<String, Object> requestBody) {
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
