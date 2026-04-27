package com.rehearse.api.infra.ai.prompt;

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
        builder = new ResumeInterviewPlannerPromptBuilder("gpt-4o-mini", 0.3, 2048);
        builder.init();
    }

    @Test
    @DisplayName("build_callType_is_resume_interview_planner")
    void build_callType_is_resume_interview_planner() {
        ChatRequest request = builder.build("{}", 30, "MID", "resume_interview_planner");

        assertThat(request.callType()).isEqualTo("resume_interview_planner");
    }

    @Test
    @DisplayName("build_modelOverride_is_gpt4o_mini")
    void build_modelOverride_is_gpt4o_mini() {
        ChatRequest request = builder.build("{}", 30, "MID", "resume_interview_planner");

        assertThat(request.modelOverride()).isEqualTo("gpt-4o-mini");
    }

    @Test
    @DisplayName("build_temperature_is_0_3_and_maxTokens_is_2048")
    void build_temperature_is_0_3_and_maxTokens_is_2048() {
        ChatRequest request = builder.build("{}", 30, "MID", "resume_interview_planner");

        assertThat(request.temperature()).isEqualTo(0.3);
        assertThat(request.maxTokens()).isEqualTo(2048);
    }

    @Test
    @DisplayName("build_responseFormat_is_json_object")
    void build_responseFormat_is_json_object() {
        ChatRequest request = builder.build("{}", 30, "MID", "resume_interview_planner");

        assertThat(request.responseFormat()).isEqualTo(ResponseFormat.JSON_OBJECT);
    }

    @Test
    @DisplayName("build_user_message_replaces_skeleton_json_slot")
    void build_user_message_replaces_skeleton_json_slot() {
        String skeletonJson = "{\"resume_id\":\"r_test1234\"}";
        ChatRequest request = builder.build(skeletonJson, 30, "MID", "resume_interview_planner");

        String userMessage = request.messages().get(0).content();
        assertThat(userMessage).contains(skeletonJson);
        assertThat(userMessage).doesNotContain("{{SKELETON_JSON}}");
    }

    @Test
    @DisplayName("build_user_message_replaces_duration_min_slot")
    void build_user_message_replaces_duration_min_slot() {
        ChatRequest request = builder.build("{}", 45, "MID", "resume_interview_planner");

        String userMessage = request.messages().get(0).content();
        assertThat(userMessage).contains("45");
        assertThat(userMessage).doesNotContain("{{DURATION_MIN}}");
    }

    @Test
    @DisplayName("build_user_message_contains_forbidden_fields_warning")
    void build_user_message_contains_forbidden_fields_warning() {
        ChatRequest request = builder.build("{}", 30, "MID", "resume_interview_planner");

        String userMessage = request.messages().get(0).content();
        assertThat(userMessage).contains("allocated_time_min");
        assertThat(userMessage).contains("max_turns");
        assertThat(userMessage).contains("estimated_duration_min");
    }
}
