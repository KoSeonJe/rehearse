package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.entity.Perspective;
import com.rehearse.api.domain.question.entity.ReferenceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnswerAnalyzerPromptBuilder - Step A 프롬프트 빌더")
class AnswerAnalyzerPromptBuilderTest {

    private AnswerAnalyzerPromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new AnswerAnalyzerPromptBuilder();
        builder.init();
    }

    @Test
    @DisplayName("system_prompt_contains_security_rules_and_json_contract")
    void system_prompt_contains_security_rules_and_json_contract() {
        String system = builder.buildSystemPrompt();

        assertThat(system).contains("보안 규칙");
        assertThat(system).contains("<<<USER_ANSWER>>>");
        assertThat(system).contains("recommended_next_action");
        assertThat(system).contains("DEEP_DIVE");
        assertThat(system).contains("CLARIFICATION");
        assertThat(system).contains("missing_perspectives");
    }

    @Test
    @DisplayName("user_prompt_uses_concept_label_when_reference_is_model_answer")
    void user_prompt_uses_concept_label_when_reference_is_model_answer() {
        String user = builder.buildUserPrompt(
                "GC를 설명해주세요.",
                ReferenceType.MODEL_ANSWER,
                "Young/Old 세대 분리합니다.",
                List.of()
        );

        assertThat(user).contains("QUESTION_REFERENCE_TYPE: CONCEPT");
        assertThat(user).contains("ASKED_PERSPECTIVES: (없음)");
        assertThat(user).contains("<<<USER_ANSWER>>>");
        assertThat(user).contains("Young/Old 세대 분리합니다.");
        assertThat(user).contains("<<<MAIN_QUESTION>>>");
        assertThat(user).contains("GC를 설명해주세요.");
    }

    @Test
    @DisplayName("user_prompt_uses_experience_label_when_reference_is_guide")
    void user_prompt_uses_experience_label_when_reference_is_guide() {
        String user = builder.buildUserPrompt(
                "캐시 도입 경험을 설명해주세요.",
                ReferenceType.GUIDE,
                "Redis Cache-Aside 적용",
                List.of(Perspective.TRADEOFF, Perspective.COLLABORATION)
        );

        assertThat(user).contains("QUESTION_REFERENCE_TYPE: EXPERIENCE");
        assertThat(user).contains("ASKED_PERSPECTIVES: TRADEOFF, COLLABORATION");
    }

    @Test
    @DisplayName("user_prompt_defaults_to_concept_when_reference_is_null")
    void user_prompt_defaults_to_concept_when_reference_is_null() {
        String user = builder.buildUserPrompt("Q", null, "A", null);

        assertThat(user).contains("QUESTION_REFERENCE_TYPE: CONCEPT");
        assertThat(user).contains("ASKED_PERSPECTIVES: (없음)");
    }

    @Test
    @DisplayName("user_prompt_handles_null_main_question_and_answer_safely")
    void user_prompt_handles_null_main_question_and_answer_safely() {
        String user = builder.buildUserPrompt(null, ReferenceType.MODEL_ANSWER, null, List.of());

        assertThat(user).contains("(없음)");
    }
}
