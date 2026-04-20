package com.rehearse.api.infra.ai.dto;

/**
 * AI 응답 포맷 지정.
 * JSON_OBJECT: OpenAI response_format={"type":"json_object"}, Claude JSON 강제 지시
 * TEXT: 일반 텍스트 응답
 */
public enum ResponseFormat {
    JSON_OBJECT,
    TEXT
}
