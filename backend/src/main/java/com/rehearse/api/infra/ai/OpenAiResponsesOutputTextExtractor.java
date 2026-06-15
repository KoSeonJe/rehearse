package com.rehearse.api.infra.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiResponsesOutputTextExtractor {

    private static final int PREVIEW_MAX_LENGTH = 80;

    private final ObjectMapper objectMapper;

    public String extract(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            log.error("[OpenAI Responses] 응답 본문 비어있음");
            throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
        }

        JsonNode root = parseTree(responseBody);
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
                if (!"output_text".equals(part.path("type").asText(""))) {
                    continue;
                }
                String text = part.path("text").asText("");
                if (!text.isBlank()) {
                    return text;
                }
            }
        }

        log.error("[OpenAI Responses] output_text 추출 실패: bodyLen={}, bodyPreview={}",
                responseBody.length(), maskPreview(responseBody));
        throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
    }

    private JsonNode parseTree(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException e) {
            log.error("[OpenAI Responses] 응답 JSON 파싱 실패", e);
            throw new BusinessException(AiErrorCode.RESPONSE_INVALID);
        }
    }

    private String maskPreview(String text) {
        if (text == null) {
            return "(null)";
        }
        if (text.length() <= PREVIEW_MAX_LENGTH) {
            return text;
        }
        return text.substring(0, PREVIEW_MAX_LENGTH) + "...";
    }
}
