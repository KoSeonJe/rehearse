package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.ChainStep;
import com.rehearse.api.domain.resume.entity.InterrogationChain;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.StepType;
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

@DisplayName("PlaygroundModeHandler Service Integration - 적재 검증")
class PlaygroundModeHandlerIntegrationTest extends ServiceIntegrationSupport {

    @Autowired
    private PlaygroundModeHandler handler;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private ResilientAiClient resilientAiClient;

    @Test
    @DisplayName("handleOpener 호출 시 RESUME_OPENER row 가 tts_text/best_answer 적재된다")
    void handleOpener_persistsAllFields() {
        Long interviewId = persistInterview();
        ResumeSkeleton skeleton = createSkeleton();
        InterviewPlan plan = createPlan();
        InterviewRuntimeState state = new InterviewRuntimeState("JUNIOR", skeleton);
        given(resilientAiClient.chat(any())).willReturn(openerChatResponse());

        PlaygroundModeHandler.OpenerResult result = handler.handleOpener(interviewId, state, skeleton, plan);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT question_type, tts_text, best_answer FROM question WHERE id = ?",
                result.questionId());
        assertThat(row.get("question_type")).isEqualTo("RESUME_OPENER");
        assertThat((String) row.get("tts_text")).hasSizeGreaterThanOrEqualTo(10);
        assertThat((String) row.get("best_answer")).isNotBlank();
    }

    @Test
    @DisplayName("handle (responder) 호출 시 RESUME_PLAYGROUND row 가 정합 적재된다")
    void handle_responder_persistsAllFields() {
        Long interviewId = persistInterview();
        ResumeSkeleton skeleton = createSkeleton();
        InterviewPlan plan = createPlan();
        InterviewRuntimeState state = new InterviewRuntimeState("JUNIOR", skeleton);
        given(resilientAiClient.chat(any())).willReturn(responderChatResponse());

        PlaygroundModeHandler.PlaygroundTurnResult result =
                handler.handle(interviewId, state, "답변입니다", null, skeleton, plan, List.of());

        assertThat(result.questionId()).isNotNull();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT question_type, tts_text, best_answer FROM question WHERE id = ?",
                result.questionId());
        assertThat(row.get("question_type")).isEqualTo("RESUME_PLAYGROUND");
        assertThat((String) row.get("tts_text")).hasSizeGreaterThanOrEqualTo(10);
        assertThat((String) row.get("best_answer")).isNotBlank();
    }

    private Long persistInterview() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("playground@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-pg")
                .role(UserRole.USER)
                .build());
        Interview interview = TestFixtures.createInterview(
                user.getId(), Position.BACKEND, InterviewLevel.JUNIOR, List.of(InterviewType.RESUME_BASED));
        interviewRepository.saveAndFlush(interview);
        return interview.getId();
    }

    private ResumeSkeleton createSkeleton() {
        ChainStep what = new ChainStep(1, StepType.WHAT, "Redis 란?");
        ChainStep how = new ChainStep(2, StepType.HOW, "어떻게 사용했나?");
        ChainStep whyMech = new ChainStep(3, StepType.WHY_MECH, "왜 Redis 인가?");
        ChainStep tradeoff = new ChainStep(4, StepType.TRADEOFF, "트레이드오프?");
        InterrogationChain chain = new InterrogationChain("Redis 캐싱", 0.9, List.of(what, how, whyMech, tradeoff));
        Project project = new Project("proj1", "Redis 캐싱 프로젝트",
                List.of(), "", "", List.of(), List.of(), List.of(chain));
        return new ResumeSkeleton("resume1", "hash", null, "backend", List.of(project), java.util.Map.of());
    }

    private InterviewPlan createPlan() {
        ChainReference ref = new ChainReference("proj1::redis", "Redis", 1, List.of(1, 2, 3, 4));
        PlaygroundPhase pg = new PlaygroundPhase("프로젝트 소개해주세요", List.of("Redis 사용", "캐싱 전략"));
        InterrogationPhase ip = new InterrogationPhase(List.of(ref), List.of());
        ProjectPlan pp = new ProjectPlan("proj1", "Redis 캐싱 프로젝트", 1, pg, ip);
        return new InterviewPlan("plan-001", List.of(pp));
    }

    private ChatResponse openerChatResponse() {
        String json = """
                {
                  "question": "Redis 캐싱 프로젝트 소개해주세요",
                  "tts_question": "Redis 캐싱 프로젝트 소개해 주세요",
                  "reason": "오프너 시작",
                  "best_answer": "프로젝트의 배경, 본인 역할, 가장 인상 깊었던 기술적/비기술적 경험을 STAR 구조로 1~2문단 정리하세요."
                }
                """;
        return new ChatResponse(json, ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
    }

    private ChatResponse responderChatResponse() {
        String json = """
                {
                  "question": "그 결정이 왜 Redis 였나요?",
                  "tts_question": "그 결정이 왜 Redis 였나요",
                  "reason": "근거 추적",
                  "should_switch_to_interrogation": false,
                  "switch_conditions_met": {"a_covered": false, "b_length_ok": false, "c_signal": false, "d_turn_limit": false},
                  "best_answer": "방금 답변 중 가장 결정적이었던 선택과 그 근거를 짧은 일화로 풀어내고, 결과·배운 점까지 한 줄로 잇는 게 좋습니다."
                }
                """;
        return new ChatResponse(json, ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
    }
}
