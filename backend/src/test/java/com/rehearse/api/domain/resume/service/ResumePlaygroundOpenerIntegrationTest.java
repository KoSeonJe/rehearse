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
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundOpenerResult;
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
        Project project = new Project("proj-1", "Test Project", List.of(), List.of());
        PlaygroundPhase phase = new PlaygroundPhase("프로젝트에 대해 자유롭게 소개해주세요.", List.of("c1", "c2"));

        builder.buildOpener(42L, state, project, phase);

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        org.mockito.Mockito.verify(aiClient).chat(captor.capture());
        List<ChatMessage> messages = captor.getValue().messages();

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
}
