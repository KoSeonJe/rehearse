package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.question.service.KeywordMatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordMatcherTest {

    private final KeywordMatcher keywordMatcher = new KeywordMatcher();

    @Nested
    @DisplayName("matches 메서드 — null/empty 방어")
    class NullAndEmptyGuard {

        @Test
        @DisplayName("keywords가 null이면 false를 반환한다")
        void matches_nullKeywords_returnsFalse() {
            // given
            List<String> keywords = null;

            // when
            boolean result = keywordMatcher.matches(keywords, 1, "Spring IoC는 제어의 역전입니다");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("answer가 null이면 false를 반환한다")
        void matches_nullAnswer_returnsFalse() {
            // given
            String answer = null;

            // when
            boolean result = keywordMatcher.matches(List.of("Spring", "IoC"), 1, answer);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("keywords가 빈 리스트이면 false를 반환한다")
        void matches_emptyKeywords_returnsFalse() {
            // given
            List<String> keywords = List.of();

            // when
            boolean result = keywordMatcher.matches(keywords, 1, "Spring IoC는 제어의 역전입니다");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("answer가 빈 문자열이면 매칭이 되지 않아 false를 반환한다")
        void matches_emptyAnswer_returnsFalse() {
            // given
            String answer = "";

            // when
            boolean result = keywordMatcher.matches(List.of("Spring", "IoC"), 2, answer);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("matches 메서드 — threshold 검증")
    class ThresholdCheck {

        @Test
        @DisplayName("매칭 수가 threshold 미만이면 false를 반환한다")
        void matches_belowThreshold_returnsFalse() {
            // given
            List<String> keywords = List.of("Spring", "IoC", "DI");
            String answer = "Spring은 IoC를 지원합니다";
            int threshold = 3;

            // when
            boolean result = keywordMatcher.matches(keywords, threshold, answer);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("매칭 수가 threshold 이상이면 true를 반환한다")
        void matches_meetsThreshold_returnsTrue() {
            // given
            List<String> keywords = List.of("Spring", "IoC", "DI");
            String answer = "Spring은 IoC와 DI를 지원합니다";
            int threshold = 3;

            // when
            boolean result = keywordMatcher.matches(keywords, threshold, answer);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("threshold가 keywords.size()를 초과하면 모든 키워드가 매칭되어도 false를 반환한다")
        void matches_thresholdExceedsSize_returnsFalse() {
            // given
            List<String> keywords = List.of("Spring", "IoC");
            String answer = "Spring은 IoC를 지원합니다";
            int threshold = 3;

            // when
            boolean result = keywordMatcher.matches(keywords, threshold, answer);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("matches 메서드 — 매칭 방식")
    class MatchingBehavior {

        @Test
        @DisplayName("대소문자를 무시하고 매칭한다")
        void matches_caseInsensitive_returnsTrue() {
            // given
            List<String> keywords = List.of("spring", "ioc");
            String answer = "SPRING은 IOC를 지원합니다";
            int threshold = 2;

            // when
            boolean result = keywordMatcher.matches(keywords, threshold, answer);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("부분 문자열도 매칭으로 카운트된다")
        void matches_substringMatch_counted() {
            // given
            List<String> keywords = List.of("Spring");
            String answer = "SpringBoot는 Spring Framework를 기반으로 합니다";
            int threshold = 1;

            // when
            boolean result = keywordMatcher.matches(keywords, threshold, answer);

            // then
            assertThat(result).isTrue();
        }
    }
}
