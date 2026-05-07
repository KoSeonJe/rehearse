package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.domain.user.repository.UserRepository;
import com.rehearse.api.global.support.TestFixtures;
import com.rehearse.api.infra.ai.ResilientAiClient;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@DisplayName("WrapUpModeHandler Service Integration - 4 필드 적재 검증")
class WrapUpModeHandlerIntegrationTest extends ServiceIntegrationSupport {

    @Autowired
    private WrapUpModeHandler handler;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private ResilientAiClient resilientAiClient;

    @Test
    @DisplayName("handle 호출 시 RESUME_WRAP_UP row 가 4 필드 정합 적재된다 (tts_text/model_answer 적재, reference_type/feedback_perspective NULL)")
    void handle_persistsAllFourFields() {
        Long interviewId = persistInterview();
        InterviewPlan plan = createPlan();
        InterviewRuntimeState state = new InterviewRuntimeState("JUNIOR", null);
        given(resilientAiClient.chat(any())).willReturn(wrapUpChatResponse());

        WrapUpModeHandler.WrapUpTurnResult result =
                handler.handle(interviewId, state, "답변", createAnalysis(), plan, 3L, true, List.of());

        assertThat(result.questionId()).isNotNull();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT question_type, tts_text, model_answer, reference_type, feedback_perspective FROM question WHERE id = ?",
                result.questionId());
        assertThat(row.get("question_type")).isEqualTo("RESUME_WRAP_UP");
        assertThat((String) row.get("tts_text")).hasSizeGreaterThanOrEqualTo(10);
        assertThat((String) row.get("model_answer")).isNotBlank();
        assertThat(row.get("reference_type")).isNull();
        assertThat(row.get("feedback_perspective")).isNull();
    }

    private Long persistInterview() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("wrapup@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-wu")
                .role(UserRole.USER)
                .build());
        Interview interview = TestFixtures.createInterview(
                user.getId(), Position.BACKEND, InterviewLevel.JUNIOR, List.of(InterviewType.RESUME_BASED));
        interviewRepository.saveAndFlush(interview);
        return interview.getId();
    }

    private InterviewPlan createPlan() {
        ChainReference ref = new ChainReference("proj1::redis", "Redis", 1, List.of(1, 2, 3, 4));
        PlaygroundPhase pg = new PlaygroundPhase("프로젝트 소개", List.of());
        InterrogationPhase ip = new InterrogationPhase(List.of(ref), List.of());
        ProjectPlan pp = new ProjectPlan("proj1", "Redis Cache", 1, pg, ip);
        return new InterviewPlan("plan-001", List.of(pp));
    }

    private AnswerAnalysis createAnalysis() {
        return new AnswerAnalysis(1L, List.of(), List.of(), List.of(), 3, RecommendedNextAction.DEEP_DIVE);
    }

    private ChatResponse wrapUpChatResponse() {
        String json = """
                {
                  "question": "마지막으로 덧붙이고 싶은 내용 있나요?",
                  "tts_question": "마지막으로 덧붙이고 싶은 내용 있나요",
                  "reason": "회고 마무리",
                  "is_wrap_up_question": true,
                  "session_complete": false,
                  "model_answer": "이번 면접에서 새로 깨달은 점, 이후 보강하고 싶은 주제, 마무리 한마디를 짧게 정리해 회고하세요."
                }
                """;
        return new ChatResponse(json, ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
    }
}
