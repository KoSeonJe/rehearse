package com.rehearse.api.infra.ai.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.rubric.entity.RubricDimension;
import com.rehearse.api.domain.feedback.rubric.service.RubricCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("RubricScorerPromptBuilder - verbal 채점 프롬프트 빌더")
class RubricScorerPromptBuilderTest {

    private RubricScorerPromptBuilder builder;
    private String templateContent;

    @BeforeEach
    void setUp() throws Exception {
        RubricCatalog rubricCatalog = mock(RubricCatalog.class);
        when(rubricCatalog.getAllDimensions()).thenReturn(Map.<String, RubricDimension>of());

        ResourceLoader resourceLoader = new DefaultResourceLoader();
        builder = new RubricScorerPromptBuilder(rubricCatalog, resourceLoader, new ObjectMapper());
        builder.init();

        templateContent = resourceLoader
                .getResource("classpath:prompts/template/turn-rubric-scorer.txt")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("buildSystemPrompt - system 메시지 한국어 강제")
    class BuildSystemPrompt {

        @Test
        @DisplayName("system prompt 에 '한국어' 역할 지시가 포함된다")
        void buildSystemPrompt_contains_korean_role() {
            String system = invokeBuildSystemPrompt();

            assertThat(system).contains("한국어");
            assertThat(system).contains("observation");
            assertThat(system).contains("evidence_quote");
        }
    }

    @Nested
    @DisplayName("template - verbatim / FORBIDDEN 강화 룰 보존")
    class Template {

        @Test
        @DisplayName("template 에 verbatim substring + FORBIDDEN 강화 룰이 포함된다")
        void build_prompt_contains_verbatim_rule() {
            assertThat(templateContent).contains("verbatim substring");
            assertThat(templateContent).contains("FORBIDDEN");
        }

        @Test
        @DisplayName("template 에 영어 Output Schema 예시가 잔존하지 않는다")
        void template_does_not_contain_english_schema_example() {
            assertThat(templateContent)
                    .doesNotContain("The candidate explained the principle");
        }

        @Test
        @DisplayName("template 에 사용자 답변 보안 마커가 보존된다")
        void template_preserves_user_answer_markers() {
            assertThat(templateContent).contains("<<<USER_ANSWER>>>");
            assertThat(templateContent).contains("<<<END_USER_ANSWER>>>");
        }
    }

    private String invokeBuildSystemPrompt() {
        try {
            var method = RubricScorerPromptBuilder.class.getDeclaredMethod("buildSystemPrompt");
            method.setAccessible(true);
            return (String) method.invoke(builder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
