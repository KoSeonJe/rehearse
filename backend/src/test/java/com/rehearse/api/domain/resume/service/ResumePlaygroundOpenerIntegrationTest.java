package com.rehearse.api.domain.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.global.config.ContextEngineeringProperties;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.context.layer.DialogueHistoryLayer;
import com.rehearse.api.infra.ai.context.layer.FixedContextLayer;
import com.rehearse.api.infra.ai.context.layer.FocusLayer;
import com.rehearse.api.infra.ai.context.layer.SessionStateLayer;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.context.token.TokenEstimator;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundOpenerResult;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundResponderResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Resume playground opener — 4 layer Context Engineering 통합 검증.
 * 실제 L1/L2/L3/L4 레이어를 사용하여 ChatRequest.messages 가 모든 layer 출력을 포함하는지 확인.
 */
class ResumePlaygroundOpenerIntegrationTest {

    @Test
    @DisplayName("buildOpener 호출 시 L1(SYSTEM cached) + L2(SESSION STATE) + L4(USER fragment) 모두 ChatRequest.messages 에 주입된다")
    void opener_invocation_assembles_all_four_layers() {
        TestContext ctx = newContext();

        Project project = new Project("proj-1", "샘플 프로젝트", List.of(), List.of());
        PlaygroundPhase phase = new PlaygroundPhase("프로젝트에 대해 자유롭게 소개해주세요.", List.of("c1", "c2"));

        ctx.builder.buildOpener(42L, ctx.state, project, phase);

        List<ChatMessage> messages = capturedMessages(ctx);

        assertThat(messages).isNotEmpty();
        // L1: cached SYSTEM
        assertThat(messages.stream().anyMatch(m ->
                m.role() == ChatMessage.Role.SYSTEM
                        && m.cacheControl()
                        && m.content().contains("보안 규칙")))
                .as("L1 cached SYSTEM 블록 누락")
                .isTrue();
        // L2: SESSION STATE header
        assertThat(messages.stream().anyMatch(m ->
                m.role() == ChatMessage.Role.SYSTEM
                        && m.content().startsWith("## SESSION STATE")))
                .as("L2 SESSION STATE 블록 누락 (runtimeState 미주입)")
                .isTrue();
        // L4: USER fragment with PROJECT_INFO + opener question
        assertThat(messages.stream().anyMatch(m ->
                m.role() == ChatMessage.Role.USER
                        && m.content().contains("<<<PROJECT_INFO>>>")
                        && m.content().contains("프로젝트에 대해 자유롭게 소개해주세요.")))
                .as("L4 PROJECT_INFO + opener question 블록 누락 (focusHints 미주입)")
                .isTrue();
    }

    @Test
    @DisplayName("Project.projectName 명시 시 L4 USER fragment 의 PROJECT_INFO 블록에 정확한 명칭이 포함된다 — production substitution 회귀 가드")
    void opener_invocation_propagates_explicit_project_name_to_l4_fragment() {
        TestContext ctx = newContext();

        String explicitName = "결제 시스템 안정화 프로젝트";
        Project project = new Project("proj-explicit", explicitName, List.of(), List.of());
        PlaygroundPhase phase = new PlaygroundPhase("프로젝트 흐름 소개해주세요.", List.of());

        ctx.builder.buildOpener(99L, ctx.state, project, phase);

        List<ChatMessage> messages = capturedMessages(ctx);
        ChatMessage userFragment = messages.stream()
                .filter(m -> m.role() == ChatMessage.Role.USER && m.content().contains("<<<PROJECT_INFO>>>"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("L4 USER fragment 누락"));

        assertThat(userFragment.content())
                .contains("projectName: " + explicitName)
                .doesNotContain("projectName: null")
                .doesNotContain("projectId:")
                .doesNotContain("claims:")
                .doesNotContain("implicitCsTopics:");
    }

    @Test
    @DisplayName("Project.projectName 부재 시 L4 USER fragment 에 빈 명칭 라인이 포함되며 'null' 문자열이나 임의 명칭이 새지 않는다")
    void opener_invocation_passes_blank_project_name_through_without_hallucination() {
        TestContext ctx = newContext();

        Project project = new Project("proj-blank", null, List.of(), List.of());
        PlaygroundPhase phase = new PlaygroundPhase("자유롭게 이야기해주세요.", List.of());

        ctx.builder.buildOpener(7L, ctx.state, project, phase);

        List<ChatMessage> messages = capturedMessages(ctx);
        ChatMessage userFragment = messages.stream()
                .filter(m -> m.role() == ChatMessage.Role.USER && m.content().contains("<<<PROJECT_INFO>>>"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("L4 USER fragment 누락"));

        assertThat(userFragment.content())
                .contains("projectName:")
                .doesNotContain("projectName: null")
                .doesNotContain("projectId:")
                .doesNotContain("claims:")
                .doesNotContain("implicitCsTopics:");
    }

    @Test
    @DisplayName("buildResponder 호출 시 L4 USER fragment PROJECT_INFO 슬롯도 projectName 1라인만 포함한다 (카운트 라벨 부재)")
    void responder_invocation_propagates_simplified_project_info() {
        TestContext ctx = newContext();
        given(ctx.parser.parseOrRetry(any(), any(), any(), any()))
                .willReturn(new PlaygroundResponderResult(
                        "후속 질문", "후속 질문", "이유", false,
                        new PlaygroundResponderResult.SwitchConditions(false, false, false, false)));

        String explicitName = "검색 인덱싱 파이프라인";
        Project project = new Project("proj-responder", explicitName, List.of(), List.of());

        ctx.builder.buildResponder(
                123L, ctx.state, List.<FollowUpExchange>of(),
                project, "방금 그 부분 더 말씀해주세요.", List.of("c1", "c2"),
                1, 120);

        List<ChatMessage> messages = capturedMessages(ctx);
        ChatMessage userFragment = messages.stream()
                .filter(m -> m.role() == ChatMessage.Role.USER && m.content().contains("<<<PROJECT_INFO>>>"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("L4 USER fragment 누락"));

        assertThat(userFragment.content())
                .contains("projectName: " + explicitName)
                .doesNotContain("projectId:")
                .doesNotContain("claims:")
                .doesNotContain("implicitCsTopics:");
    }

    private static TestContext newContext() {
        TokenEstimator tokenEstimator = new TokenEstimator();
        ObjectMapper objectMapper = new ObjectMapper();

        FixedContextLayer l1 = new FixedContextLayer();
        l1.init();
        SessionStateLayer l2 = new SessionStateLayer(objectMapper, tokenEstimator);
        DialogueHistoryLayer l3 = mock(DialogueHistoryLayer.class);
        given(l3.build(any())).willReturn(List.of());
        FocusLayer l4 = new FocusLayer(tokenEstimator);

        ContextEngineeringProperties props = new ContextEngineeringProperties(true, 5, 5, true, 8000);
        ContextEngineeringMetrics metrics = new ContextEngineeringMetrics(new SimpleMeterRegistry());
        InterviewContextBuilder contextBuilder = new InterviewContextBuilder(
                l1, l2, l3, l4, tokenEstimator, props, metrics);

        AiClient aiClient = mock(AiClient.class);
        AiResponseParser parser = mock(AiResponseParser.class);
        ChatResponse stub = new ChatResponse(
                "{\"question\":\"q\",\"ttsQuestion\":\"q\",\"reason\":\"r\"}",
                ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
        given(aiClient.chat(any())).willReturn(stub);
        given(parser.parseOrRetry(any(), any(), any(), any()))
                .willReturn(new PlaygroundOpenerResult("q", "q", "r"));

        ResumePlaygroundPromptBuilder builder = new ResumePlaygroundPromptBuilder(
                aiClient, parser, contextBuilder, "gpt-4o-mini", 0.7, 800);

        ResumeSkeleton skeleton = new ResumeSkeleton("r1", "hash", CandidateLevel.MID, "backend", List.of(), null);
        InterviewRuntimeState state = new InterviewRuntimeState("MID", skeleton);

        return new TestContext(builder, aiClient, parser, state);
    }

    private static List<ChatMessage> capturedMessages(TestContext ctx) {
        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        org.mockito.Mockito.verify(ctx.aiClient).chat(captor.capture());
        return captor.getValue().messages();
    }

    private record TestContext(
            ResumePlaygroundPromptBuilder builder,
            AiClient aiClient,
            AiResponseParser parser,
            InterviewRuntimeState state
    ) {}
}
