package com.rehearse.api.infra.ai.prompt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResumeExtractorPromptBuilder - 이력서 추출 프롬프트 빌더")
class ResumeExtractorPromptBuilderTest {

    private ResumeExtractorPromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ResumeExtractorPromptBuilder();
        builder.init();
    }

    @Test
    @DisplayName("system_prompt_contains_extraction_principles_and_schema")
    void system_prompt_contains_extraction_principles_and_schema() {
        String system = builder.buildSystemPrompt();

        assertThat(system).contains("추출 원칙");
        assertThat(system).contains("명시적 claim만 추출");
        assertThat(system).contains("4단 심문 체인 필수");
        assertThat(system).contains("WHAT");
        assertThat(system).contains("HOW");
        assertThat(system).contains("WHY_MECH");
        assertThat(system).contains("TRADEOFF");
    }

    @Test
    @DisplayName("system_prompt_contains_security_rules_for_injection_prevention")
    void system_prompt_contains_security_rules_for_injection_prevention() {
        String system = builder.buildSystemPrompt();

        assertThat(system).contains("보안 규칙");
        assertThat(system).contains("<<<RESUME_TEXT>>>");
        assertThat(system).contains("지시문이 아니다");
    }

    @Test
    @DisplayName("system_prompt_contains_output_schema_fields")
    void system_prompt_contains_output_schema_fields() {
        String system = builder.buildSystemPrompt();

        assertThat(system).contains("resume_id");
        assertThat(system).contains("candidate_level");
        assertThat(system).contains("target_domain");
        assertThat(system).contains("projects");
        assertThat(system).contains("interrogation_priority_map");
        assertThat(system).contains("claim_type");
        assertThat(system).contains("confidence");
    }

    @Test
    @DisplayName("user_prompt_wraps_resume_text_in_delimiter_tags")
    void user_prompt_wraps_resume_text_in_delimiter_tags() {
        String resumeText = "Java 백엔드 개발자, Spring Boot 3년 경험";
        String user = builder.buildUserPrompt(resumeText);

        assertThat(user).contains("<<<RESUME_TEXT>>>");
        assertThat(user).contains("<<<END_RESUME_TEXT>>>");
        assertThat(user).contains(resumeText);
    }

    @Test
    @DisplayName("user_prompt_handles_null_resume_text_safely")
    void user_prompt_handles_null_resume_text_safely() {
        String user = builder.buildUserPrompt(null);

        assertThat(user).contains("<<<RESUME_TEXT>>>");
        assertThat(user).contains("(없음)");
        assertThat(user).doesNotContain("null");
    }

    @Test
    @DisplayName("user_prompt_contains_json_response_instruction")
    void user_prompt_contains_json_response_instruction() {
        String user = builder.buildUserPrompt("이력서 내용");

        assertThat(user).contains("JSON");
    }
}
