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

    /**
     * JSON 파싱 실패 시 1회 재호출 후 재시도하는 파싱 메서드.
     *
     * <p>1차 파싱 실패 시 {@code retryCall} Supplier 를 실행해 새 응답을 받아 다시 파싱한다.
     * 2차 파싱도 실패하면 {@link BusinessException}(PARSE_FAILED) 를 던진다.</p>
     *
     * @param text      1차 파싱 대상 텍스트
     * @param clazz     파싱 대상 클래스
     * @param retryCall 재호출 Supplier — 호출측이 "이전 응답은 스키마를 위반" 프롬프트를 주입한 새 ChatResponse 를 반환해야 함
     * @param <T>       파싱 결과 타입
     * @return 파싱된 객체
     */
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

    /**
     * 고수준 parseOrRetry — ChatResponse 기반 자동 재호출.
     *
     * <p>1차: {@code initial.content()} 파싱 시도.
     * 실패 시 {@code originalRequest.withSchemaRetryHint(violation)} 으로 {@code client.chat()} 재호출
     * → 2차 파싱 시도. 2차도 실패하면 {@link BusinessException}(PARSE_FAILED) 를 던진다.</p>
     *
     * @param initial         최초 AI 응답
     * @param clazz           파싱 대상 클래스
     * @param client          재호출에 사용할 AiClient (JSON_OBJECT 요청이어야 효과적)
     * @param originalRequest 재호출 시 스키마 힌트를 덧붙일 원본 요청
     * @param <T>             파싱 결과 타입
     * @return 파싱된 객체
     */
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
