package com.rehearse.api.infra.ai.dto;

public record ChatResponse(
        String content,
        Usage usage,
        String provider,
        String model,
        boolean cacheHit,
        // fallbackUsed 는 ResilientAiClient 만 true 로 set. provider 단일 호출은 항상 false.
        boolean fallbackUsed
) {

    public record Usage(
            int inputTokens,
            int outputTokens,
            int cacheReadTokens,
            int cacheWriteTokens
    ) {
        public static Usage of(int inputTokens, int outputTokens) {
            return new Usage(inputTokens, outputTokens, 0, 0);
        }

        public static Usage of(int inputTokens, int outputTokens, int cacheRead, int cacheWrite) {
            return new Usage(inputTokens, outputTokens, cacheRead, cacheWrite);
        }

        public static Usage empty() {
            return new Usage(0, 0, 0, 0);
        }
    }
}
