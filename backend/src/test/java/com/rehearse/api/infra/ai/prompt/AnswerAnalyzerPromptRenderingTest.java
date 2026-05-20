package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.question.entity.ReferenceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnswerAnalyzerPromptBuilder — 차원 기반 분석 prompt 렌더링")
class AnswerAnalyzerPromptRenderingTest {

    private AnswerAnalyzerPromptBuilder builder;
    private String templateContent;

    @BeforeEach
    void setUp() throws Exception {
        builder = new AnswerAnalyzerPromptBuilder();
        builder.init();

        templateContent = new DefaultResourceLoader()
                .getResource("classpath:prompts/template/answer-analyzer.txt")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("template — 차원 기반 스키마 정합성")
    class Template {

        @Test
        @DisplayName("dimension_gaps / weakest_dimension 토큰 포함")
        void template_contains_dimension_tokens() {
            assertThat(templateContent).contains("dimension_gaps");
            assertThat(templateContent).contains("weakest_dimension");
        }

        @Test
        @DisplayName("폐기된 missing_perspectives / asked_perspectives 토큰 부재")
        void template_does_not_contain_deprecated_perspective_tokens() {
            assertThat(templateContent).doesNotContain("missing_perspectives");
            assertThat(templateContent).doesNotContain("asked_perspectives");
        }

        @Test
        @DisplayName("CS / Resume 트랙 라벨 안내가 template 에 포함된다")
        void template_contains_track_labels() {
            assertThat(templateContent).contains("TRACK: CS");
            assertThat(templateContent).contains("TRACK: RESUME");
        }

        @Test
        @DisplayName("Resume 전용 차원 키가 template 에 안내된다")
        void template_describes_resume_only_dimensions() {
            assertThat(templateContent).contains("factual_consistency");
            assertThat(templateContent).contains("chain_depth");
        }
    }

    @Nested
    @DisplayName("buildSystemPrompt — 시스템 메시지")
    class BuildSystemPrompt {

        @Test
        @DisplayName("system prompt 가 template 전체와 동일")
        void buildSystemPrompt_returns_template_verbatim() {
            assertThat(builder.buildSystemPrompt()).isEqualTo(templateContent);
        }
    }

    @Nested
    @DisplayName("buildUserPrompt — 사용자 메시지")
    class BuildUserPrompt {

        @Test
        @DisplayName("user prompt 에 사용자 답변 보안 마커가 포함된다")
        void buildUserPrompt_preserves_user_answer_markers() {
            String user = builder.buildUserPrompt(
                    "OS 가상메모리 동작 설명해주세요",
                    ReferenceType.MODEL_ANSWER,
                    "페이지 테이블을 통해..."
            );

            assertThat(user).contains("<<<USER_ANSWER>>>");
            assertThat(user).contains("<<<END_USER_ANSWER>>>");
            assertThat(user).contains("<<<MAIN_QUESTION>>>");
            assertThat(user).contains("<<<END_MAIN_QUESTION>>>");
            assertThat(user).contains("QUESTION_REFERENCE_TYPE:");
        }

        @Test
        @DisplayName("user prompt 에 폐기 토큰 부재")
        void buildUserPrompt_does_not_contain_deprecated_tokens() {
            String user = builder.buildUserPrompt(
                    "프로젝트 설명해주세요",
                    ReferenceType.GUIDE,
                    "주문 시스템을 만들었습니다."
            );

            assertThat(user).doesNotContain("missing_perspectives");
            assertThat(user).doesNotContain("asked_perspectives");
        }
    }
}
