package com.rehearse.api.infra.ai.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GeneratedQuestionsWrapper - LLM 응답 매핑 시점 검증")
class GeneratedQuestionsWrapperTest {

    private GeneratedQuestion sampleQuestion() {
        return new GeneratedQuestion(
                "질문", "tts", "cat", 1, "criteria", "BEHAVIORAL", "model", "strategy");
    }

    @Nested
    @DisplayName("정상 매핑")
    class HappyPath {

        @Test
        @DisplayName("questions 가 있으면 인스턴스를 생성한다")
        void should_construct_when_questions_present() {
            GeneratedQuestionsWrapper w = new GeneratedQuestionsWrapper(List.of(sampleQuestion()));
            assertThat(w.questions()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("매핑 거절")
    class Rejection {

        @Test
        @DisplayName("questions 가 null 이면 거절한다")
        void should_reject_when_questions_null() {
            assertThatThrownBy(() -> new GeneratedQuestionsWrapper(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("questions 가 비어있으면 거절한다")
        void should_reject_when_questions_empty() {
            assertThatThrownBy(() -> new GeneratedQuestionsWrapper(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
