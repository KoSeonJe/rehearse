package com.rehearse.api.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContextEngineeringProperties - 설정 바인딩 및 유효성 검증")
class ContextEngineeringPropertiesTest {

    @Test
    @DisplayName("binds_from_yaml_values_correctly")
    void binds_from_yaml_values_correctly() {
        ContextEngineeringProperties props = new ContextEngineeringProperties(true, 5, 5, true, 8000);

        assertThat(props.l1Caching()).isTrue();
        assertThat(props.l3CompactionThreshold()).isEqualTo(5);
        assertThat(props.l3RecentWindow()).isEqualTo(5);
        assertThat(props.l4JustInTime()).isTrue();
        assertThat(props.maxContextTokens()).isEqualTo(8000);
    }

    @Test
    @DisplayName("rejects_recent_window_greater_than_threshold")
    void rejects_recent_window_greater_than_threshold() {
        assertThatThrownBy(() -> new ContextEngineeringProperties(true, 5, 6, true, 8000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("l3RecentWindow must be <= l3CompactionThreshold");
    }

    @Test
    @DisplayName("accepts_recent_window_equal_to_threshold")
    void accepts_recent_window_equal_to_threshold() {
        ContextEngineeringProperties props = new ContextEngineeringProperties(true, 5, 5, true, 8000);
        assertThat(props.l3RecentWindow()).isEqualTo(5);
        assertThat(props.l3CompactionThreshold()).isEqualTo(5);
    }

    @Test
    @DisplayName("accepts_recent_window_less_than_threshold")
    void accepts_recent_window_less_than_threshold() {
        ContextEngineeringProperties props = new ContextEngineeringProperties(true, 10, 5, true, 8000);
        assertThat(props.l3RecentWindow()).isEqualTo(5);
        assertThat(props.l3CompactionThreshold()).isEqualTo(10);
    }
}
