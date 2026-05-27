package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.question.entity.QuestionCategory;
import com.rehearse.api.domain.question.entity.ReferenceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AudioTurnAnalyzerPromptBuilder — system 은 카테고리별 키셋 규칙, user 는 CATEGORY 라벨을 조립한다")
class AudioTurnAnalyzerPromptBuilderTest {

    private AudioTurnAnalyzerPromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new AudioTurnAnalyzerPromptBuilder();
        builder.init();
    }

    @Nested
    @DisplayName("system 프롬프트 — 카테고리별 dimension 키셋 규칙")
    class SystemPrompt {

        @Test
        @DisplayName("system 의 카테고리별 키 집합 규칙에 CONCEPT 5키 / EXPERIENCE 4키 / RESUME 10키가 명시된다")
        void system_declares_category_keysets() {
            String system = builder.buildSystemPrompt();

            assertThat(system)
                    .contains("CATEGORY: CONCEPT")
                    .contains("CATEGORY: EXPERIENCE")
                    .contains("CATEGORY: RESUME");
        }

        @Test
        @DisplayName("CONCEPT 키 집합 줄에 5키만 나열되고 experience_concreteness / collaboration_awareness 가 빠진다")
        void concept_keyset_excludes_experience_dimensions() {
            String conceptLine = keysetLine("CATEGORY: CONCEPT");

            assertThat(conceptLine)
                    .contains("technical_depth")
                    .contains("reasoning_communication")
                    .contains("conceptual_accuracy")
                    .contains("practical_application")
                    .contains("recovery_from_gaps")
                    .doesNotContain("experience_concreteness")
                    .doesNotContain("collaboration_awareness");
        }

        @Test
        @DisplayName("EXPERIENCE 키 집합 줄에 4키 (problem_framing / reasoning_communication / experience_concreteness / collaboration_awareness) 만 나열된다")
        void experience_keyset_has_four_keys() {
            String experienceLine = keysetLine("CATEGORY: EXPERIENCE");

            assertThat(experienceLine)
                    .contains("problem_framing")
                    .contains("reasoning_communication")
                    .contains("experience_concreteness")
                    .contains("collaboration_awareness")
                    .doesNotContain("technical_depth")
                    .doesNotContain("chain_depth");
        }

        @Test
        @DisplayName("RESUME 키 집합 줄에 factual_consistency / chain_depth 를 포함한 10키가 유지된다")
        void resume_keyset_keeps_ten_keys() {
            String resumeLine = keysetLine("CATEGORY: RESUME");

            assertThat(resumeLine)
                    .contains("factual_consistency")
                    .contains("chain_depth")
                    .contains("experience_concreteness")
                    .contains("collaboration_awareness");
        }

        private String keysetLine(String marker) {
            return builder.buildSystemPrompt().lines()
                    .filter(line -> line.startsWith("- `" + marker + "`"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("키 집합 줄 없음: " + marker));
        }
    }

    @Nested
    @DisplayName("user 프롬프트 — CATEGORY 라벨")
    class UserPrompt {

        @Test
        @DisplayName("user 에 CATEGORY 라벨 / MAIN_QUESTION / QUESTION_REFERENCE_TYPE 마커가 포함된다")
        void user_contains_markers() {
            String user = builder.buildUserPromptText("주요 질문", ReferenceType.MODEL_ANSWER, QuestionCategory.CONCEPT);

            assertThat(user)
                    .contains("CATEGORY: CONCEPT")
                    .contains("<<<MAIN_QUESTION>>>")
                    .contains("주요 질문")
                    .contains("QUESTION_REFERENCE_TYPE: MODEL_ANSWER");
        }

        @Test
        @DisplayName("category 별 CATEGORY 라벨이 enum name 그대로 렌더된다")
        void user_renders_category_label() {
            assertThat(builder.buildUserPromptText("Q", ReferenceType.MODEL_ANSWER, QuestionCategory.CONCEPT))
                    .contains("CATEGORY: CONCEPT");
            assertThat(builder.buildUserPromptText("Q", ReferenceType.GUIDE, QuestionCategory.EXPERIENCE))
                    .contains("CATEGORY: EXPERIENCE");
            assertThat(builder.buildUserPromptText("Q", ReferenceType.GUIDE, QuestionCategory.RESUME))
                    .contains("CATEGORY: RESUME");
        }
    }
}
