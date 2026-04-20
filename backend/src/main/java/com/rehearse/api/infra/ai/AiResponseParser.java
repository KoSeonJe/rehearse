package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    public <T> T parseJsonResponse(String text, Class<T> clazz) {
        try {
            String json = extractJson(text);
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("AI 응답 JSON 파싱 실패: {}", text, e);
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }
    }

    public <T> T parseWithRetry(String text, Class<T> clazz, Supplier<ChatResponse> retryCall) {
        try {
            String json = extractJson(text);
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException firstEx) {
            log.warn("AI 응답 1차 파싱 실패, 재호출 시도: {}", firstEx.getMessage());
            try {
                ChatResponse retryResponse = retryCall.get();
                String retryJson = extractJson(retryResponse.content());
                return objectMapper.readValue(retryJson, clazz);
            } catch (JsonProcessingException secondEx) {
                log.error("AI 응답 2차 파싱도 실패: {}", secondEx.getMessage());
                throw new BusinessException(AiErrorCode.PARSE_FAILED);
            }
        }
    }

    // 1차 파싱 실패 시 originalRequest 에 스키마 힌트를 덧붙여 client.chat() 재호출 → 2차 파싱.
    public <T> T parseOrRetry(ChatResponse initial, Class<T> clazz, AiClient client, ChatRequest originalRequest) {
        try {
            String json = extractJson(initial.content());
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException firstEx) {
            log.warn("AI 응답 1차 파싱 실패, 스키마 힌트 재호출 시도: {}", firstEx.getMessage());
            try {
                ChatRequest retryRequest = originalRequest.withSchemaRetryHint(firstEx.getMessage());
                ChatResponse retryResponse = client.chat(retryRequest);
                String retryJson = extractJson(retryResponse.content());
                return objectMapper.readValue(retryJson, clazz);
            } catch (JsonProcessingException secondEx) {
                log.error("AI 응답 2차 파싱도 실패: {}", secondEx.getMessage());
                throw new BusinessException(AiErrorCode.PARSE_FAILED);
            }
        }
    }

    public String extractJson(String text) {
        String json = text;
        if (json.contains("```json")) {
            json = json.substring(json.indexOf("```json") + 7);
            int closingIdx = json.indexOf("```");
            if (closingIdx >= 0) {
                json = json.substring(0, closingIdx);
            } else {
                log.warn("닫는 ``` 없음 (응답 잘림 가능성). 전체 텍스트를 JSON으로 파싱 시도합니다.");
            }
        } else if (json.contains("```")) {
            json = json.substring(json.indexOf("```") + 3);
            int closingIdx = json.indexOf("```");
            if (closingIdx >= 0) {
                json = json.substring(0, closingIdx);
            } else {
                log.warn("닫는 ``` 없음 (응답 잘림 가능성). 전체 텍스트를 JSON으로 파싱 시도합니다.");
            }
        }
        return json.trim();
    }
}
