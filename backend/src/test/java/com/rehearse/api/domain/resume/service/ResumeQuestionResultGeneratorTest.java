package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder.InterrogationResult;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundOpenerResult;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundResponderResult;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundResponderResult.SwitchConditions;
import com.rehearse.api.infra.ai.prompt.ResumeWrapUpPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumeWrapUpPromptBuilder.WrapUpResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeQuestionResultGenerator - modelAnswer 검증 / 1회 retry / 폴백")
class ResumeQuestionResultGeneratorTest {

    @InjectMocks
    private ResumeQuestionResultGenerator generator;

    @Mock
    private ResumePlaygroundPromptBuilder playgroundPromptBuilder;
    @Mock
    private ResumeChainInterrogatorPromptBuilder chainInterrogatorPromptBuilder;
    @Mock
    private ResumeWrapUpPromptBuilder wrapUpPromptBuilder;

    private InterviewRuntimeState state;
    private Project project;
    private PlaygroundPhase phase;

    @BeforeEach
    void setUp() {
        state = new InterviewRuntimeState("JUNIOR", null);
        project = new Project("proj1", "Redis 캐싱 프로젝트", List.of(), List.of());
        phase = new PlaygroundPhase("프로젝트 소개해주세요", List.of("c1"));
    }

    @Nested
    @DisplayName("generateOpener - opener modelAnswer 검증")
    class Opener {

        @Test
        @DisplayName("정상 modelAnswer 반환 시 1회 호출 + 결과 그대로 반환")
        void opener_validModelAnswer_returnsAsIs() {
            PlaygroundOpenerResult result = new PlaygroundOpenerResult("Q", "Q", "R", "정상 가이드");
            given(playgroundPromptBuilder.buildOpener(any(), any(), any(), any())).willReturn(result);

            PlaygroundOpenerResult actual = generator.generateOpener(1L, state, project, phase);

            assertThat(actual.modelAnswer()).isEqualTo("정상 가이드");
            then(playgroundPromptBuilder).should(times(1)).buildOpener(any(), any(), any(), any());
        }

        @Test
        @DisplayName("1차 blank + 2차 정상 시 2회 호출 + 2차 결과 반환")
        void opener_firstBlankSecondValid_retriesOnce() {
            PlaygroundOpenerResult blank = new PlaygroundOpenerResult("Q", "Q", "R", "  ");
            PlaygroundOpenerResult valid = new PlaygroundOpenerResult("Q", "Q", "R", "재시도 가이드");
            given(playgroundPromptBuilder.buildOpener(any(), any(), any(), any()))
                    .willReturn(blank, valid);

            PlaygroundOpenerResult actual = generator.generateOpener(1L, state, project, phase);

            assertThat(actual.modelAnswer()).isEqualTo("재시도 가이드");
            then(playgroundPromptBuilder).should(times(2)).buildOpener(any(), any(), any(), any());
        }

        @Test
        @DisplayName("1차 + 2차 모두 blank 시 폴백 텍스트 적용 결과를 반환한다 (다른 필드 보존)")
        void opener_bothBlank_appliesFallback() {
            PlaygroundOpenerResult blank1 = new PlaygroundOpenerResult("Q1", "T1", "R1", null);
            PlaygroundOpenerResult blank2 = new PlaygroundOpenerResult("Q2", "T2", "R2", "");
            given(playgroundPromptBuilder.buildOpener(any(), any(), any(), any()))
                    .willReturn(blank1, blank2);

            PlaygroundOpenerResult actual = generator.generateOpener(1L, state, project, phase);

            assertThat(actual.modelAnswer()).isEqualTo(ResumeFallbackModelAnswers.OPENER);
            assertThat(actual.question()).isEqualTo("Q2");
            assertThat(actual.ttsQuestion()).isEqualTo("T2");
            assertThat(actual.reason()).isEqualTo("R2");
            then(playgroundPromptBuilder).should(times(2)).buildOpener(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("generatePlaygroundResponder - responder modelAnswer 검증")
    class Responder {

        private final SwitchConditions cond = new SwitchConditions(false, false, false, false);

        @Test
        @DisplayName("정상 modelAnswer 반환 시 1회 호출 + 결과 그대로 반환")
        void responder_validModelAnswer_returnsAsIs() {
            PlaygroundResponderResult result =
                    new PlaygroundResponderResult("Q", "Q", "R", false, cond, "정상 가이드");
            given(playgroundPromptBuilder.buildResponder(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(result);

            PlaygroundResponderResult actual = generator.generatePlaygroundResponder(
                    1L, state, List.of(), project, "answer", List.of(), 0, 0);

            assertThat(actual.modelAnswer()).isEqualTo("정상 가이드");
            then(playgroundPromptBuilder).should(times(1))
                    .buildResponder(any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("1차 blank + 2차 정상 시 2회 호출 + 2차 결과 반환")
        void responder_firstBlankSecondValid_retriesOnce() {
            PlaygroundResponderResult blank =
                    new PlaygroundResponderResult("Q", "Q", "R", false, cond, null);
            PlaygroundResponderResult valid =
                    new PlaygroundResponderResult("Q", "Q", "R", false, cond, "재시도 가이드");
            given(playgroundPromptBuilder.buildResponder(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(blank, valid);

            PlaygroundResponderResult actual = generator.generatePlaygroundResponder(
                    1L, state, List.of(), project, "answer", List.of(), 0, 0);

            assertThat(actual.modelAnswer()).isEqualTo("재시도 가이드");
            then(playgroundPromptBuilder).should(times(2))
                    .buildResponder(any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("1차 + 2차 모두 blank 시 폴백 텍스트 적용 결과를 반환한다 (다른 필드 보존)")
        void responder_bothBlank_appliesFallback() {
            PlaygroundResponderResult blank1 =
                    new PlaygroundResponderResult("Q1", "T1", "R1", true, cond, null);
            PlaygroundResponderResult blank2 =
                    new PlaygroundResponderResult("Q2", "T2", "R2", false, cond, "");
            given(playgroundPromptBuilder.buildResponder(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(blank1, blank2);

            PlaygroundResponderResult actual = generator.generatePlaygroundResponder(
                    1L, state, List.of(), project, "answer", List.of(), 0, 0);

            assertThat(actual.modelAnswer()).isEqualTo(ResumeFallbackModelAnswers.PLAYGROUND);
            assertThat(actual.question()).isEqualTo("Q2");
            assertThat(actual.shouldSwitchToInterrogation()).isFalse();
            assertThat(actual.switchConditionsMet()).isSameAs(cond);
            then(playgroundPromptBuilder).should(times(2))
                    .buildResponder(any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
        }
    }

    @Nested
    @DisplayName("generateInterrogation - interrogation modelAnswer 검증")
    class Interrogation {

        @Test
        @DisplayName("정상 modelAnswer 반환 시 1회 호출 + 결과 그대로 반환")
        void interrogation_validModelAnswer_returnsAsIs() {
            InterrogationResult result = new InterrogationResult("Q", "Q", "R", "LEVEL_UP", 2, "정상 가이드");
            given(chainInterrogatorPromptBuilder.build(
                    anyLong(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(result);

            InterrogationResult actual = generator.generateInterrogation(
                    1L, state, List.of(), "Redis", "topic", 1, 4, "answer", 0);

            assertThat(actual.modelAnswer()).isEqualTo("정상 가이드");
            then(chainInterrogatorPromptBuilder).should(times(1))
                    .build(anyLong(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt());
        }

        @Test
        @DisplayName("1차 blank + 2차 정상 시 2회 호출 + 2차 결과 반환")
        void interrogation_firstBlankSecondValid_retriesOnce() {
            InterrogationResult blank = new InterrogationResult("Q", "Q", "R", "LEVEL_UP", 2, "");
            InterrogationResult valid = new InterrogationResult("Q", "Q", "R", "LEVEL_UP", 2, "재시도 가이드");
            given(chainInterrogatorPromptBuilder.build(
                    anyLong(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(blank, valid);

            InterrogationResult actual = generator.generateInterrogation(
                    1L, state, List.of(), "Redis", "topic", 1, 4, "answer", 0);

            assertThat(actual.modelAnswer()).isEqualTo("재시도 가이드");
            then(chainInterrogatorPromptBuilder).should(times(2))
                    .build(anyLong(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt());
        }

        @Test
        @DisplayName("1차 + 2차 모두 blank 시 폴백 텍스트 적용 결과를 반환한다 (next_action / level 보존)")
        void interrogation_bothBlank_appliesFallback() {
            InterrogationResult blank1 = new InterrogationResult("Q1", "T1", "R1", "LEVEL_STAY", 1, null);
            InterrogationResult blank2 = new InterrogationResult("Q2", "T2", "R2", "CHAIN_SWITCH", 1, " ");
            given(chainInterrogatorPromptBuilder.build(
                    anyLong(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(blank1, blank2);

            InterrogationResult actual = generator.generateInterrogation(
                    1L, state, List.of(), "Redis", "topic", 1, 4, "answer", 0);

            assertThat(actual.modelAnswer()).isEqualTo(ResumeFallbackModelAnswers.INTERROGATION);
            assertThat(actual.question()).isEqualTo("Q2");
            assertThat(actual.nextAction()).isEqualTo("CHAIN_SWITCH");
            assertThat(actual.isChainSwitch()).isTrue();
            then(chainInterrogatorPromptBuilder).should(times(2))
                    .build(anyLong(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("generateWrapUp - wrap-up modelAnswer 검증")
    class WrapUp {

        @Test
        @DisplayName("정상 modelAnswer 반환 시 1회 호출 + 결과 그대로 반환")
        void wrapUp_validModelAnswer_returnsAsIs() {
            WrapUpResult result = new WrapUpResult("Q", "Q", "R", true, false, "정상 가이드");
            given(wrapUpPromptBuilder.build(any(), any(), any(), any(), anyLong(), anyBoolean()))
                    .willReturn(result);

            WrapUpResult actual = generator.generateWrapUp(
                    1L, state, List.of(), "summary", 5L, true);

            assertThat(actual.modelAnswer()).isEqualTo("정상 가이드");
            then(wrapUpPromptBuilder).should(times(1))
                    .build(any(), any(), any(), any(), anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("1차 blank + 2차 정상 시 2회 호출 + 2차 결과 반환")
        void wrapUp_firstBlankSecondValid_retriesOnce() {
            WrapUpResult blank = new WrapUpResult("Q", "Q", "R", true, false, null);
            WrapUpResult valid = new WrapUpResult("Q", "Q", "R", true, false, "재시도 가이드");
            given(wrapUpPromptBuilder.build(any(), any(), any(), any(), anyLong(), anyBoolean()))
                    .willReturn(blank, valid);

            WrapUpResult actual = generator.generateWrapUp(
                    1L, state, List.of(), "summary", 5L, true);

            assertThat(actual.modelAnswer()).isEqualTo("재시도 가이드");
            then(wrapUpPromptBuilder).should(times(2))
                    .build(any(), any(), any(), any(), anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("1차 + 2차 모두 blank 시 폴백 텍스트 적용 결과를 반환한다 (sessionComplete 보존)")
        void wrapUp_bothBlank_appliesFallback() {
            WrapUpResult blank1 = new WrapUpResult("Q1", "T1", "R1", true, true, "");
            WrapUpResult blank2 = new WrapUpResult("Q2", "T2", "R2", true, false, null);
            given(wrapUpPromptBuilder.build(any(), any(), any(), any(), anyLong(), anyBoolean()))
                    .willReturn(blank1, blank2);

            WrapUpResult actual = generator.generateWrapUp(
                    1L, state, List.of(), "summary", 5L, true);

            assertThat(actual.modelAnswer()).isEqualTo(ResumeFallbackModelAnswers.WRAP_UP);
            assertThat(actual.question()).isEqualTo("Q2");
            assertThat(actual.sessionComplete()).isFalse();
            then(wrapUpPromptBuilder).should(times(2))
                    .build(any(), any(), any(), any(), anyLong(), anyBoolean());
        }
    }
}
