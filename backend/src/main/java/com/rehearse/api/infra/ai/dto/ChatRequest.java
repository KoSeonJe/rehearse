package com.rehearse.api.infra.ai.dto;

import java.util.ArrayList;
import java.util.List;

public record ChatRequest(
        List<ChatMessage> messages,
        String modelOverride,
        Double temperature,
        Integer maxTokens,
        CachePolicy cachePolicy,
        ResponseFormat responseFormat,
        JsonSchemaSpec jsonSchema,
        String callType
) {

    public ChatRequest {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("ChatRequest.messages 는 비어있을 수 없습니다.");
        }
        if (cachePolicy == null) {
            cachePolicy = CachePolicy.defaults();
        }
        if (responseFormat == null) {
            responseFormat = ResponseFormat.TEXT;
        }
        if (responseFormat == ResponseFormat.JSON_SCHEMA && jsonSchema == null) {
            throw new IllegalArgumentException("responseFormat=JSON_SCHEMA 시 jsonSchema 는 필수입니다.");
        }
        if (callType == null || callType.isBlank()) {
            callType = "unknown";
        }
    }

    public ChatRequest withCachePolicy(CachePolicy newCachePolicy) {
        return new ChatRequest(messages, modelOverride, temperature, maxTokens,
                newCachePolicy, responseFormat, jsonSchema, callType);
    }

    public ChatRequest withSchemaRetryHint(String violation) {
        return withSchemaRetryHint(violation, null);
    }

    public ChatRequest withSchemaRetryHint(String violation, String schemaExample) {
        StringBuilder hint = new StringBuilder("이전 응답이 JSON 스키마를 위반했습니다: ")
                .append(violation)
                .append(".");
        if (schemaExample != null && !schemaExample.isBlank()) {
            hint.append("\n반드시 아래 형태의 JSON 객체로 응답하세요. 배열 안 항목 타입(객체 vs 문자열)을 절대 바꾸지 마세요.\n```json\n")
                .append(schemaExample.strip())
                .append("\n```");
        } else {
            hint.append(" 동일 요청을 올바른 JSON 객체로만 다시 생성하세요.");
        }
        List<ChatMessage> newMessages = new ArrayList<>(messages);
        newMessages.add(ChatMessage.of(ChatMessage.Role.USER, hint.toString()));
        return new ChatRequest(List.copyOf(newMessages), modelOverride, temperature, maxTokens,
                cachePolicy, responseFormat, jsonSchema, callType);
    }

    public ChatRequest withRetryHint(String hint, String schemaExample) {
        StringBuilder body = new StringBuilder(hint == null ? "" : hint);
        if (schemaExample != null && !schemaExample.isBlank()) {
            if (body.length() > 0) {
                body.append("\n");
            }
            body.append("반드시 아래 형태의 JSON 객체로 응답하세요. 배열 안 항목 타입(객체 vs 문자열)을 절대 바꾸지 마세요.\n```json\n")
                .append(schemaExample.strip())
                .append("\n```");
        }
        List<ChatMessage> newMessages = new ArrayList<>(messages);
        newMessages.add(ChatMessage.of(ChatMessage.Role.USER, body.toString()));
        return new ChatRequest(List.copyOf(newMessages), modelOverride, temperature, maxTokens,
                cachePolicy, responseFormat, jsonSchema, callType);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<ChatMessage> messages;
        private String modelOverride;
        private Double temperature;
        private Integer maxTokens;
        private CachePolicy cachePolicy;
        private ResponseFormat responseFormat;
        private JsonSchemaSpec jsonSchema;
        private String callType;

        private Builder() {}

        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder modelOverride(String modelOverride) {
            this.modelOverride = modelOverride;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder cachePolicy(CachePolicy cachePolicy) {
            this.cachePolicy = cachePolicy;
            return this;
        }

        public Builder responseFormat(ResponseFormat responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public Builder jsonSchema(JsonSchemaSpec jsonSchema) {
            this.jsonSchema = jsonSchema;
            return this;
        }

        public Builder callType(String callType) {
            this.callType = callType;
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(messages, modelOverride, temperature, maxTokens,
                    cachePolicy, responseFormat, jsonSchema, callType);
        }
    }
}
