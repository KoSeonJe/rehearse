package com.rehearse.api.domain.interview.entity;

import com.rehearse.api.domain.interview.entity.Perspective;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AskedPerspectives — exchanges 에서 Perspective 추출 + parse + distinct")
class AskedPerspectivesTest {

    private static FollowUpExchange exchange(String perspective) {
        FollowUpExchange e = new FollowUpExchange();
        ReflectionTestUtils.setField(e, "selectedPerspective", perspective);
        return e;
    }

    @Test
    @DisplayName("null exchanges → empty")
    void from_null_returnsEmpty() {
        AskedPerspectives result = AskedPerspectives.from(null);
        assertThat(result.values()).isEmpty();
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("빈 list → empty")
    void from_emptyList_returnsEmpty() {
        AskedPerspectives result = AskedPerspectives.from(List.of());
        assertThat(result.values()).isEmpty();
    }

    @Test
    @DisplayName("정상 enum 이름 parse + distinct")
    void from_validValues_parsedAndDistinct() {
        AskedPerspectives result = AskedPerspectives.from(List.of(
                exchange("TRADEOFF"), exchange("RELIABILITY"), exchange("TRADEOFF")));
        assertThat(result.values()).containsExactly(Perspective.TRADEOFF, Perspective.RELIABILITY);
    }

    @Test
    @DisplayName("소문자/공백 → 정규화 + parse")
    void from_lowercaseWithWhitespace_normalized() {
        AskedPerspectives result = AskedPerspectives.from(List.of(
                exchange("  tradeoff  "), exchange("reliability")));
        assertThat(result.values()).containsExactly(Perspective.TRADEOFF, Perspective.RELIABILITY);
    }

    @Test
    @DisplayName("알 수 없는 enum 이름 → 무시")
    void from_invalidValue_ignored() {
        AskedPerspectives result = AskedPerspectives.from(List.of(
                exchange("UNKNOWN"), exchange("TRADEOFF"), exchange(null), exchange("")));
        assertThat(result.values()).containsExactly(Perspective.TRADEOFF);
    }

    @Test
    @DisplayName("empty() 정적 팩토리")
    void empty_returnsEmpty() {
        assertThat(AskedPerspectives.empty().values()).isEmpty();
        assertThat(AskedPerspectives.empty().isEmpty()).isTrue();
    }
}
