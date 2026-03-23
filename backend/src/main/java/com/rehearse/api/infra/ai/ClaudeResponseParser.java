package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeResponseParser {

    private final ObjectMapper objectMapper;

    public <T> T parseJsonResponse(String text, Class<T> clazz) {
        try {
            String json = extractJson(text);
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Claude 응답 JSON 파싱 실패: {}", text, e);
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI_005", "AI 응답을 파싱할 수 없습니다.");
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
