package com.rehearse.api.domain.resume.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChainRefTest {

    @Test
    @DisplayName("정상 입력으로 ChainRef 가 생성된다")
    void chainRef_정상생성() {
        ChainRef chainRef = new ChainRef("p1::캐시설계", "캐시설계", 1, List.of(1, 2, 3, 4));

        assertThat(chainRef.chainId()).isEqualTo("p1::캐시설계");
        assertThat(chainRef.topic()).isEqualTo("캐시설계");
        assertThat(chainRef.priority()).isEqualTo(1);
        assertThat(chainRef.levelsToCover()).containsExactly(1, 2, 3, 4);
    }

    @Test
    @DisplayName("levelsToCover 는 불변 리스트로 반환된다")
    void chainRef_levelsToCover_불변() {
        ChainRef chainRef = new ChainRef("p1::topic", "topic", 1, List.of(1, 2));

        assertThatThrownBy(() -> chainRef.levelsToCover().add(3))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("chainId 가 null 이면 예외가 발생한다")
    void chainRef_chainId_null_reject() {
        assertThatThrownBy(() -> new ChainRef(null, "topic", 1, List.of(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chainId 는 필수입니다");
    }

    @Test
    @DisplayName("chainId 가 blank 이면 예외가 발생한다")
    void chainRef_chainId_blank_reject() {
        assertThatThrownBy(() -> new ChainRef("  ", "topic", 1, List.of(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chainId 는 필수입니다");
    }

    @Test
    @DisplayName("chainId 에 '::' 가 없으면 예외가 발생한다")
    void chainRef_chainId_합성키_미포함_reject() {
        assertThatThrownBy(() -> new ChainRef("p1-topic", "topic", 1, List.of(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("::");
    }

    @Test
    @DisplayName("topic 이 null 이면 예외가 발생한다")
    void chainRef_topic_null_reject() {
        assertThatThrownBy(() -> new ChainRef("p1::topic", null, 1, List.of(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topic 은 필수입니다");
    }

    @Test
    @DisplayName("priority 가 1 미만이면 예외가 발생한다")
    void chainRef_priority_범위_미만_reject() {
        assertThatThrownBy(() -> new ChainRef("p1::topic", "topic", 0, List.of(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("priority 는 1 이상이어야 합니다");
    }

    @Test
    @DisplayName("levelsToCover 가 null 이면 예외가 발생한다")
    void chainRef_levelsToCover_null_reject() {
        assertThatThrownBy(() -> new ChainRef("p1::topic", "topic", 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("levelsToCover 는 필수입니다");
    }

    @Test
    @DisplayName("levelsToCover 가 빈 리스트이면 예외가 발생한다")
    void chainRef_levelsToCover_빈리스트_reject() {
        assertThatThrownBy(() -> new ChainRef("p1::topic", "topic", 1, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("levelsToCover 는 비어있을 수 없습니다");
    }

    @Test
    @DisplayName("levelsToCover 원소가 0 이면 예외가 발생한다")
    void chainRef_levelsToCover_최솟값_미만_reject() {
        assertThatThrownBy(() -> new ChainRef("p1::topic", "topic", 1, List.of(0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1~4 범위여야 합니다");
    }

    @Test
    @DisplayName("levelsToCover 원소가 5 이면 예외가 발생한다")
    void chainRef_levelsToCover_최댓값_초과_reject() {
        assertThatThrownBy(() -> new ChainRef("p1::topic", "topic", 1, List.of(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1~4 범위여야 합니다");
    }
}
