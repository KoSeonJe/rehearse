package com.rehearse.api.infra.ai.dto;

/**
 * 범용 AI 채팅 응답.
 *
 * @param content       AI 응답 텍스트
 * @param usage         토큰 사용량
 * @param provider      응답한 프로바이더 ("openai" / "claude")
 * @param model         실제 사용된 모델 ID
 * @param cacheHit      Claude 캐시 히트 여부 (cacheReadTokens > 0)
 * @param fallbackUsed  {@code true} 인 경우 {@code ResilientAiClient} 가 Claude fallback 경로를 사용한 것임.
 *                      <strong>소유주: ResilientAiClient 만 이 필드를 set 한다.</strong>
 *                      Provider 직접 호출 시({@code OpenAiClient}, {@code ClaudeApiClient})
 *                      항상 {@code false} 로 하드코딩된다.
 */
public record ChatResponse(
        String content,
        Usage usage,
        String provider,
        String model,
        boolean cacheHit,
        boolean fallbackUsed
) {

    /**
     * 토큰 사용량 상세.
     *
     * @param inputTokens       입력 토큰
     * @param outputTokens      출력 토큰
     * @param cacheReadTokens   캐시에서 읽은 토큰 (Claude cache_read_input_tokens)
     * @param cacheWriteTokens  캐시에 쓴 토큰 (Claude cache_creation_input_tokens)
     */
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
