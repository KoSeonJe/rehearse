package com.rehearse.api.domain.feedback.rubric.entity;

import jakarta.persistence.Column;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NonverbalScoreTest {

    @Test
    @DisplayName("비언어 점수 컬럼명은 루브릭 코드가 아니라 의미 중심 이름을 사용한다")
    void columnNames_useSemanticNames() throws Exception {
        assertThat(columnName("fluencyScore")).isEqualTo("fluency_score");
        assertThat(columnName("toneScore")).isEqualTo("tone_score");
        assertThat(columnName("postureScore")).isEqualTo("posture_score");
        assertThat(columnName("composureScore")).isEqualTo("composure_score");
    }

    private String columnName(String fieldName) throws Exception {
        return NonverbalScore.class.getDeclaredField(fieldName)
                .getAnnotation(Column.class)
                .name();
    }
}
