package com.rehearse.api.infra.ai.client;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.config.OpenAiCommonProperties;
import com.rehearse.api.infra.ai.config.OpenAiResumeQuestionGeneratorProperties;
import com.rehearse.api.infra.ai.dto.JsonSchemaSpec;
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
public class OpenAiResumeQuestionGeneratorClient {

    private static final String CALL_TYPE = "resume_question_generator_v2";
    private static final String PDF_FILENAME = "resume.pdf";

    private final RestClient restClient;
    private final OpenAiResumeQuestionGeneratorProperties properties;
    private final String apiKey;

    public OpenAiResumeQuestionGeneratorClient(
            @Qualifier("openAiResumeQuestionGeneratorRestClient") RestClient openAiResumeQuestionGeneratorRestClient,
            OpenAiResumeQuestionGeneratorProperties properties,
            OpenAiCommonProperties commonProperties) {
        this.restClient = openAiResumeQuestionGeneratorRestClient;
        this.properties = properties;
        this.apiKey = commonProperties.apiKey();
    }

    public String call(String systemPrompt, String userInstruction, String base64Pdf, JsonSchemaSpec schema) {
        Map<String, Object> requestBody = buildRequestBody(systemPrompt, userInstruction, base64Pdf, schema);
        return execute(requestBody);
    }

    private Map<String, Object> buildRequestBody(
            String systemPrompt, String userInstruction, String base64Pdf, JsonSchemaSpec schema) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.model());
        body.put("max_output_tokens", properties.maxTokens());
        body.put("temperature", properties.temperature());
        body.put("input", List.of(
                Map.of(
                        "role", "system",
                        "content", List.of(
                                Map.of("type", "input_text", "text", systemPrompt)
                        )
                ),
                Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "input_text", "text", userInstruction),
                                Map.of(
                                        "type", "input_file",
                                        "filename", PDF_FILENAME,
                                        "file_data", "data:application/pdf;base64," + base64Pdf
                                )
                        )
                )
        ));
        body.put("text", Map.of(
                "format", Map.of(
                        "type", "json_schema",
                        "name", schema.name(),
                        "strict", true,
                        "schema", schema.schema()
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
