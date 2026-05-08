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

@DisplayName("InterrogationModeHandler Service Integration - 적재 검증")
class InterrogationModeHandlerIntegrationTest extends ServiceIntegrationSupport {

    @Autowired
    private InterrogationModeHandler handler;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private ResilientAiClient resilientAiClient;

    @Test
    @DisplayName("handle 호출 시 RESUME_INTERROGATION row 가 tts_text/model_answer 정합 적재된다")
    void handle_persistsAllFields() {
        Long interviewId = persistInterview();
        InterviewPlan plan = createPlan();
        InterviewRuntimeState state = new InterviewRuntimeState("JUNIOR", null);
        state.getChainStateTracker().initChain("proj1", "proj1::redis");
        given(resilientAiClient.chat(any())).willReturn(interrogationChatResponse());

        InterrogationTurnResult result = handler.handle(interviewId, state, "답변", createAnalysis(), plan, List.of());

        assertThat(result.questionId()).isNotNull();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT question_type, tts_text, model_answer FROM question WHERE id = ?",
                result.questionId());
        assertThat(row.get("question_type")).isEqualTo("RESUME_INTERROGATION");
        assertThat((String) row.get("tts_text")).hasSizeGreaterThanOrEqualTo(10);
        assertThat((String) row.get("model_answer")).isNotBlank();
    }

    private Long persistInterview() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("interrogation@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-int")
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

    private ChatResponse interrogationChatResponse() {
        String json = """
                {
                  "question": "Redis 의 데이터 구조 선택 근거를 설명해주세요",
                  "tts_question": "Redis 의 데이터 구조 선택 근거를 설명해 주세요",
                  "reason": "L2 진입",
                  "next_action": "LEVEL_UP",
                  "next_level": 2,
                  "model_answer": "현재 단계 (WHAT/HOW/WHY/TRADEOFF) 에 맞춰 핵심 개념·구현 방식·선택 근거를 구분해 답하고, 마지막에 트레이드오프를 한 줄 덧붙이세요."
                }
                """;
        return new ChatResponse(json, ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
    }
}
