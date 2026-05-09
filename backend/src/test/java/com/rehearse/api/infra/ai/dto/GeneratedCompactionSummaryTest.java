package com.rehearse.api.infra.ai.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeneratedCompactionSummary - LLM 응답 매핑 정규화 + toCompactString")
class GeneratedCompactionSummaryTest {

    @Nested
    @DisplayName("정규화")
    class Normalize {

        @Test
        @DisplayName("모든 리스트 null 은 빈 리스트로 정규화된다")
        void should_normalize_all_null_lists_to_empty() {
            GeneratedCompactionSummary g = new GeneratedCompactionSummary(null, null, null, null, null);
            assertThat(g.coveredTopics()).isEmpty();
            assertThat(g.userClaimsMade()).isEmpty();
            assertThat(g.chainProgressHistory()).isEmpty();
            assertThat(g.perspectivesAsked()).isEmpty();
            assertThat(g.notableMoments()).isEmpty();
        }
    }

    @Nested
    @DisplayName("toCompactString")
    class CompactFormat {

        @Test
        @DisplayName("값이 있는 섹션만 포함되며 라벨 형식이 일관된다")
        void should_render_only_non_empty_sections() {
            GeneratedCompactionSummary g = new GeneratedCompactionSummary(
                    List.of("topic1", "topic2"), List.of(), List.of("chain1"),
                    List.of(), List.of("moment1"));

            String result = g.toCompactString();

            assertThat(result).contains("[topics] topic1 / topic2");
            assertThat(result).contains("[chain] chain1");
            assertThat(result).contains("[moments] moment1");
            assertThat(result).doesNotContain("[claims]");
            assertThat(result).doesNotContain("[perspectives]");
        }

        @Test
        @DisplayName("모든 섹션이 비면 빈 문자열을 반환한다")
        void should_return_empty_when_all_sections_empty() {
            GeneratedCompactionSummary g = new GeneratedCompactionSummary(null, null, null, null, null);
            assertThat(g.toCompactString()).isEmpty();
        }
    }
}
