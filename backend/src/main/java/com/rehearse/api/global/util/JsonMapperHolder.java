package com.rehearse.api.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonMapperHolder {

    private static ObjectMapper INSTANCE;

    public JsonMapperHolder(ObjectMapper objectMapper) {
        INSTANCE = objectMapper;
    }

    public static ObjectMapper get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("JsonMapperHolder가 아직 초기화되지 않았습니다 — Spring context 로드 전 호출 금지");
        }
        return INSTANCE;
    }
}
