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
        if (callType == null || callType.isBlank()) {
            callType = "unknown";
        }
    }

    public ChatRequest withCachePolicy(CachePolicy newCachePolicy) {
        return new ChatRequest(messages, modelOverride, temperature, maxTokens,
                newCachePolicy, responseFormat, callType);
    }

    public ChatRequest withSchemaRetryHint(String violation) {
        String hint = "이전 응답이 JSON 스키마를 위반했습니다: " + violation
                + ". 동일 요청을 올바른 JSON 객체로만 다시 생성하세요.";
        List<ChatMessage> newMessages = new ArrayList<>(messages);
        newMessages.add(ChatMessage.of(ChatMessage.Role.USER, hint));
        return new ChatRequest(List.copyOf(newMessages), modelOverride, temperature, maxTokens,
                cachePolicy, responseFormat, callType);
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

        public Builder callType(String callType) {
            this.callType = callType;
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(messages, modelOverride, temperature, maxTokens,
                    cachePolicy, responseFormat, callType);
        }
    }
}
