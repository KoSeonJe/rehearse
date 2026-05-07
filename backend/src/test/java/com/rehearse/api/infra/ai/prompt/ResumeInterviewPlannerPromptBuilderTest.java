package com.rehearse.api.infra.ai.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResumeInterviewPlannerPromptBuilder - 면접 플래너 프롬프트 빌더")
class ResumeInterviewPlannerPromptBuilderTest {

    private ResumeInterviewPlannerPromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ResumeInterviewPlannerPromptBuilder("gpt-4o-mini", 0.3, 2048, new ObjectMapper());
        builder.init();
    }

    @Test
    @DisplayName("build_callType_is_resume_interview_planner")
    void build_callType_is_resume_interview_planner() {
        ChatRequest request = builder.build(null, 30, "MID", "resume_interview_planner");

        assertThat(request.callType()).isEqualTo("resume_interview_planner");
    }

    @Test
    @DisplayName("build_modelOverride_is_gpt4o_mini")
    void build_modelOverride_is_gpt4o_mini() {
        ChatRequest request = builder.build(null, 30, "MID", "resume_interview_planner");

        assertThat(request.modelOverride()).isEqualTo("gpt-4o-mini");
    }

    @Test
    @DisplayName("build_temperature_is_0_3_and_maxTokens_is_2048")
    void build_temperature_is_0_3_and_maxTokens_is_2048() {
        ChatRequest request = builder.build(null, 30, "MID", "resume_interview_planner");

        assertThat(request.temperature()).isEqualTo(0.3);
        assertThat(request.maxTokens()).isEqualTo(2048);
    }

    @Test
    @DisplayName("build_responseFormat_is_json_object")
    void build_responseFormat_is_json_object() {
        ChatRequest request = builder.build(null, 30, "MID", "resume_interview_planner");

        assertThat(request.responseFormat()).isEqualTo(ResponseFormat.JSON_OBJECT);
    }

    @Test
    @DisplayName("build_user_message_replaces_skeleton_json_slot")
    void build_user_message_replaces_skeleton_json_slot() {
        ResumeSkeleton skeleton = new ResumeSkeleton("r_test1234", null, null, null, null, null);
        ChatRequest request = builder.build(skeleton, 30, "MID", "resume_interview_planner");

        String userMessage = request.messages().get(0).content();
        assertThat(userMessage).contains("r_test1234");
        assertThat(userMessage).doesNotContain("{{SKELETON_JSON}}");
    }

    @Test
    @DisplayName("build_user_message_replaces_duration_min_slot")
    void build_user_message_replaces_duration_min_slot() {
        ChatRequest request = builder.build(null, 45, "MID", "resume_interview_planner");

        String userMessage = request.messages().get(0).content();
        assertThat(userMessage).contains("45");
        assertThat(userMessage).doesNotContain("{{DURATION_MIN}}");
    }

    @Test
    @DisplayName("build_user_message_contains_forbidden_fields_warning")
    void build_user_message_contains_forbidden_fields_warning() {
        ChatRequest request = builder.build(null, 30, "MID", "resume_interview_planner");

        String userMessage = request.messages().get(0).content();
        assertThat(userMessage).contains("allocated_time_min");
        assertThat(userMessage).contains("max_turns");
        assertThat(userMessage).contains("estimated_duration_min");
    }

    @Test
    @DisplayName("build_systemMessage_contains_playground_opener_tone_guide — Playground intro 톤 가이드 헤더 + narrow 기술 심문 금지 어휘 포함")
    void build_systemMessage_contains_playground_opener_tone_guide() {
        ChatRequest request = builder.build(null, 30, "MID", "resume_interview_planner");

        String userMessage = request.messages().get(0).content();
        assertThat(userMessage)
                .contains("Playground opener_question 톤")
                .contains("narrow 기술 심문");
    }

    @Test
    @DisplayName("build_fewshot_opener_question_uses_intro_tone — few-shot opener_question 이 intro 톤 (역할/맡으셨/소개 1+) + narrow 어휘 부재")
    void build_fewshot_opener_question_uses_intro_tone() {
        ChatRequest request = builder.build(null, 30, "MID", "resume_interview_planner");

        String userMessage = request.messages().get(0).content();
        assertThat(userMessage)
                .containsAnyOf("역할", "맡으셨", "소개");
        assertThat(extractFewshotOpenerLine(userMessage))
                .doesNotContain("기술적 결정")
                .doesNotContain("트레이드오프");
    }

    private static String extractFewshotOpenerLine(String userMessage) {
        return userMessage.lines()
                .filter(line -> line.contains("\"opener_question\""))
                .findFirst()
                .orElseThrow(() -> new AssertionError("few-shot opener_question 라인 누락"));
    }
}
