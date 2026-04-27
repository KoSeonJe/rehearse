package com.rehearse.api.infra.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AiResponseParser.parseWithRetry() 검증.
 *
 * <p>검증 항목:
 * <ul>
 *   <li>1차 파싱 성공 시 retryCall 호출 없음</li>
 *   <li>1차 실패 → 재호출 → 2차 성공 시 결과 반환</li>
 *   <li>1차 실패 → 재호출 → 2차도 실패 시 PARSE_FAILED BusinessException</li>
 *   <li>retryCall 은 정확히 1회만 호출됨</li>
 * </ul>
 */
class AiResponseParserRetryTest {

    private AiResponseParser parser;

    record SimpleDto(String name, int value) {}

    @BeforeEach
    void setUp() {
        parser = new AiResponseParser(
                new ObjectMapper(),
                new SchemaExampleRegistry(),
                org.mockito.Mockito.mock(AiCallMetrics.class));
    }

    @Test
    @DisplayName("1차 파싱 성공 — retryCall 호출 없음")
    void parseWithRetry_firstSucceeds_noRetry() {
        String validJson = """
                {"name": "테스트", "value": 42}
                """;

        AtomicInteger retryCallCount = new AtomicInteger(0);
        Supplier<ChatResponse> retryCall = () -> {
            retryCallCount.incrementAndGet();
            return new ChatResponse("{}", ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
        };

        SimpleDto result = parser.parseWithRetry(validJson, SimpleDto.class, retryCall);

        assertThat(result.name()).isEqualTo("테스트");
        assertThat(result.value()).isEqualTo(42);
        assertThat(retryCallCount.get()).isZero();
    }

    @Test
    @DisplayName("1차 실패 → 재호출 → 2차 성공 — 결과 반환")
    void parseWithRetry_firstFails_secondSucceeds() {
        String invalidJson = "이것은 JSON이 아닙니다";
        String validJson = """
                {"name": "재시도 성공", "value": 99}
                """;

        ChatResponse retryResponse = new ChatResponse(validJson, ChatResponse.Usage.empty(), "claude", "claude-sonnet-4-20250514", false, false);
        AtomicInteger retryCallCount = new AtomicInteger(0);
        Supplier<ChatResponse> retryCall = () -> {
            retryCallCount.incrementAndGet();
            return retryResponse;
        };

        SimpleDto result = parser.parseWithRetry(invalidJson, SimpleDto.class, retryCall);

        assertThat(result.name()).isEqualTo("재시도 성공");
        assertThat(result.value()).isEqualTo(99);
        assertThat(retryCallCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("1차 실패 → 재호출 → 2차도 실패 — PARSE_FAILED 예외")
    void parseWithRetry_bothFail_throwsParseFailed() {
        String invalidJson = "첫 번째 잘못된 응답";
        String alsoInvalidJson = "두 번째도 잘못된 응답";

        ChatResponse retryResponse = new ChatResponse(alsoInvalidJson, ChatResponse.Usage.empty(), "claude", "model", false, false);
        Supplier<ChatResponse> retryCall = () -> retryResponse;

        assertThatThrownBy(() -> parser.parseWithRetry(invalidJson, SimpleDto.class, retryCall))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(AiErrorCode.PARSE_FAILED.getCode()));
    }

    @Test
    @DisplayName("retryCall 은 정확히 1회만 호출됨")
    void parseWithRetry_retryCalledExactlyOnce() {
        String invalidJson = "잘못된 JSON";
        String validJson = """
                {"name": "한 번만", "value": 1}
                """;

        AtomicInteger callCount = new AtomicInteger(0);
        Supplier<ChatResponse> retryCall = () -> {
            callCount.incrementAndGet();
            return new ChatResponse(validJson, ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
        };

        parser.parseWithRetry(invalidJson, SimpleDto.class, retryCall);

        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("1차 성공 시 retryCall 은 0회 호출")
    void parseWithRetry_firstSucceeds_retryCalledZeroTimes() {
        String validJson = """
                {"name": "즉시 성공", "value": 0}
                """;

        AtomicInteger callCount = new AtomicInteger(0);
        Supplier<ChatResponse> retryCall = () -> {
            callCount.incrementAndGet();
            return new ChatResponse("{}", ChatResponse.Usage.empty(), "openai", "model", false, false);
        };

        SimpleDto result = parser.parseWithRetry(validJson, SimpleDto.class, retryCall);

        assertThat(result.name()).isEqualTo("즉시 성공");
        assertThat(callCount.get()).isZero();
    }

    @Test
    @DisplayName("JSON 코드블록 포함 응답 — 1차 성공")
    void parseWithRetry_jsonCodeBlock_firstSucceeds() {
        String wrappedJson = """
                ```json
                {"name": "코드블록", "value": 7}
                ```
                """;

        AtomicInteger callCount = new AtomicInteger(0);
        Supplier<ChatResponse> retryCall = () -> {
            callCount.incrementAndGet();
            return new ChatResponse("{}", ChatResponse.Usage.empty(), "openai", "model", false, false);
        };

        SimpleDto result = parser.parseWithRetry(wrappedJson, SimpleDto.class, retryCall);

        assertThat(result.name()).isEqualTo("코드블록");
        assertThat(result.value()).isEqualTo(7);
        assertThat(callCount.get()).isZero();
    }
}
