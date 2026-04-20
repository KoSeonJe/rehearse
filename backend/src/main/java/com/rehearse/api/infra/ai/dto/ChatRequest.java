package com.rehearse.api.infra.ai.dto;

import java.util.List;

/**
 * 범용 AI 채팅 요청.
 *
 * @param messages       메시지 목록 (SYSTEM / USER / ASSISTANT)
 * @param modelOverride  null 이면 application.yml 기본값 사용, 비어있지 않으면 해당 모델로 override
 * @param temperature    null 이면 provider 기본값 사용
 * @param maxTokens      null 이면 provider 기본값 사용
 * @param cachePolicy    캐시 정책 (null 이면 CachePolicy.defaults())
 * @param responseFormat 응답 포맷 (null 이면 TEXT)
 * @param callType       Micrometer 태그용 호출 식별자 (예: "generate_questions", "intent_classifier")
 */
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

    /** cachePolicy 만 교체한 새 ChatRequest 반환 */
    public ChatRequest withCachePolicy(CachePolicy newCachePolicy) {
        return new ChatRequest(messages, modelOverride, temperature, maxTokens,
                newCachePolicy, responseFormat, callType);
    }

    /**
     * JSON 스키마 위반 재시도 힌트를 USER 메시지로 추가한 새 ChatRequest 반환.
     *
     * <p>AiResponseParser 가 파싱에 실패했을 때, caller 가 AI 에게 동일 요청을 올바른 JSON 으로
     * 다시 생성하도록 유도하는 메시지를 추가한다. messages 는 불변 목록이므로 새 목록을 생성한다.</p>
     *
     * @param violation 파싱 실패 사유 (예: Jackson 예외 메시지 요약)
     */
    public ChatRequest withSchemaRetryHint(String violation) {
        String hint = "이전 응답이 JSON 스키마를 위반했습니다: " + violation
                + ". 동일 요청을 올바른 JSON 객체로만 다시 생성하세요.";
        List<ChatMessage> newMessages = new java.util.ArrayList<>(messages);
        newMessages.add(ChatMessage.of(ChatMessage.Role.USER, hint));
        return new ChatRequest(java.util.List.copyOf(newMessages), modelOverride, temperature, maxTokens,
                cachePolicy, responseFormat, callType);
    }

    // ---- 빌더 편의 메서드 ----

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
