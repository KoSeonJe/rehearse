package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.ResilientAiClient;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder.InterrogationResult;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundOpenerResult;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundResponderResult;
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@DisplayName("ResumeQuestionResultGenerator Service Integration - modelAnswer 검증 / 1회 retry / 폴백")
class ResumeQuestionResultGeneratorTest extends ServiceIntegrationSupport {

    @Autowired
    private ResumeQuestionResultGenerator generator;

    @MockitoBean
    private ResilientAiClient resilientAiClient;

    private InterviewRuntimeState state;
    private Project project;
    private PlaygroundPhase phase;

    @BeforeEach
    void setUp() {
        state = new InterviewRuntimeState("JUNIOR", null);
        project = new Project("proj1", "Redis 캐싱 프로젝트",
                List.of(), "", "", List.of(), List.of(), List.of());
        phase = new PlaygroundPhase("프로젝트 소개해주세요", List.of("c1"));
    }

    @Nested
    @DisplayName("generateOpener - opener modelAnswer 검증")
    class Opener {

        @Test
        @DisplayName("정상 modelAnswer 반환 시 1회 호출 + 결과 그대로 반환")
        void opener_validModelAnswer_returnsAsIs() {
            given(resilientAiClient.chat(any()))
                    .willReturn(openerChatResponse("정상 가이드 답변입니다."));

            PlaygroundOpenerResult actual = generator.generateOpener(1L, state, project, phase);

            assertThat(actual.modelAnswer()).isEqualTo("정상 가이드 답변입니다.");
            assertThat(actual.question()).isEqualTo("Redis 프로젝트 소개해주세요");
            then(resilientAiClient).should(times(1)).chat(any());
        }

        @Test
        @DisplayName("1차 modelAnswer blank + 2차 정상 시 2회 호출 + 2차 결과 반환")
        void opener_firstBlankSecondValid_retriesOnce() {
            given(resilientAiClient.chat(any()))
                    .willReturn(openerChatResponse(" "), openerChatResponse("재시도 가이드 답변"));

            PlaygroundOpenerResult actual = generator.generateOpener(1L, state, project, phase);

            assertThat(actual.modelAnswer()).isEqualTo("재시도 가이드 답변");
            then(resilientAiClient).should(times(2)).chat(any());
        }

        @Test
        @DisplayName("1차 + 2차 모두 modelAnswer blank 이면 폴백 텍스트가 적용된다")
        void opener_bothBlank_appliesFallback() {
            given(resilientAiClient.chat(any()))
                    .willReturn(openerChatResponse(null), openerChatResponse(""));

            PlaygroundOpenerResult actual = generator.generateOpener(1L, state, project, phase);

            assertThat(actual.modelAnswer()).isEqualTo(ResumeFallbackModelAnswers.OPENER);
            assertThat(actual.question()).isEqualTo("Redis 프로젝트 소개해주세요");
            then(resilientAiClient).should(times(2)).chat(any());
        }
    }

    @Nested
    @DisplayName("generatePlaygroundResponder - responder modelAnswer 검증")
    class Responder {

        @Test
        @DisplayName("정상 modelAnswer 반환 시 1회 호출 + 결과 그대로 반환")
        void responder_validModelAnswer_returnsAsIs() {
            given(resilientAiClient.chat(any()))
                    .willReturn(responderChatResponse("정상 가이드 답변입니다.", false));

            PlaygroundResponderResult actual = generator.generatePlaygroundResponder(
                    1L, state, List.of(), project, "answer", List.of(), 0, 0);

            assertThat(actual.modelAnswer()).isEqualTo("정상 가이드 답변입니다.");
            then(resilientAiClient).should(times(1)).chat(any());
        }

        @Test
        @DisplayName("1차 modelAnswer blank + 2차 정상 시 2회 호출 + 2차 결과 반환")
        void responder_firstBlankSecondValid_retriesOnce() {
            given(resilientAiClient.chat(any()))
                    .willReturn(responderChatResponse(null, false),
                            responderChatResponse("재시도 가이드 답변", false));

            PlaygroundResponderResult actual = generator.generatePlaygroundResponder(
                    1L, state, List.of(), project, "answer", List.of(), 0, 0);

            assertThat(actual.modelAnswer()).isEqualTo("재시도 가이드 답변");
            then(resilientAiClient).should(times(2)).chat(any());
        }

        @Test
        @DisplayName("1차 + 2차 모두 modelAnswer blank 이면 폴백 텍스트가 적용된다 (다른 필드 보존)")
        void responder_bothBlank_appliesFallback() {
            given(resilientAiClient.chat(any()))
                    .willReturn(responderChatResponse("", true), responderChatResponse(" ", true));

            PlaygroundResponderResult actual = generator.generatePlaygroundResponder(
                    1L, state, List.of(), project, "answer", List.of(), 0, 0);

            assertThat(actual.modelAnswer()).isEqualTo(ResumeFallbackModelAnswers.PLAYGROUND);
            assertThat(actual.shouldSwitchToInterrogation()).isTrue();
            then(resilientAiClient).should(times(2)).chat(any());
        }
    }

    @Nested
    @DisplayName("generateInterrogation - interrogation modelAnswer 검증")
    class Interrogation {

        @Test
        @DisplayName("정상 modelAnswer 반환 시 1회 호출 + 결과 그대로 반환")
        void interrogation_validModelAnswer_returnsAsIs() {
            given(resilientAiClient.chat(any()))
                    .willReturn(interrogationChatResponse("정상 가이드 답변입니다.", "LEVEL_UP", 2));

            InterrogationResult actual = generator.generateInterrogation(
                    1L, state, List.of(), "Redis", "topic", 1, 4, "answer", 0);

            assertThat(actual.modelAnswer()).isEqualTo("정상 가이드 답변입니다.");
            assertThat(actual.nextAction()).isEqualTo("LEVEL_UP");
            then(resilientAiClient).should(times(1)).chat(any());
        }

        @Test
        @DisplayName("1차 modelAnswer blank + 2차 정상 시 2회 호출 + 2차 결과 반환")
        void interrogation_firstBlankSecondValid_retriesOnce() {
            given(resilientAiClient.chat(any()))
                    .willReturn(interrogationChatResponse("", "LEVEL_STAY", 1),
                            interrogationChatResponse("재시도 가이드 답변", "LEVEL_UP", 2));

            InterrogationResult actual = generator.generateInterrogation(
                    1L, state, List.of(), "Redis", "topic", 1, 4, "answer", 0);

            assertThat(actual.modelAnswer()).isEqualTo("재시도 가이드 답변");
            assertThat(actual.nextAction()).isEqualTo("LEVEL_UP");
            then(resilientAiClient).should(times(2)).chat(any());
        }

        @Test
        @DisplayName("1차 + 2차 모두 modelAnswer blank 이면 폴백 텍스트가 적용된다 (next_action / level 보존)")
        void interrogation_bothBlank_appliesFallback() {
            given(resilientAiClient.chat(any()))
                    .willReturn(interrogationChatResponse(null, "LEVEL_STAY", 1),
                            interrogationChatResponse(" ", "CHAIN_SWITCH", 1));

            InterrogationResult actual = generator.generateInterrogation(
                    1L, state, List.of(), "Redis", "topic", 1, 4, "answer", 0);

            assertThat(actual.modelAnswer()).isEqualTo(ResumeFallbackModelAnswers.INTERROGATION);
            assertThat(actual.nextAction()).isEqualTo("CHAIN_SWITCH");
            assertThat(actual.isChainSwitch()).isTrue();
            then(resilientAiClient).should(times(2)).chat(any());
        }
    }

    @Nested
    @DisplayName("question blank → 1회 retry → 그래도 blank 면 BusinessException(RESPONSE_INVALID) 을 던진다")
    class QuestionBlankThrow {

        @Test
        @DisplayName("opener: question 1차 + 2차 모두 blank 면 RESPONSE_INVALID 를 던진다")
        void opener_bothQuestionBlank_throwsBusinessException() {
            given(resilientAiClient.chat(any()))
                    .willReturn(openerChatResponseWithQuestion("", "model"),
                            openerChatResponseWithQuestion(" ", "model"));

            assertThatThrownBy(() -> generator.generateOpener(1L, state, project, phase))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo("AI_007"));
            then(resilientAiClient).should(times(2)).chat(any());
        }

        @Test
        @DisplayName("interrogation: question 1차 + 2차 모두 blank 면 RESPONSE_INVALID 를 던진다")
        void interrogation_bothQuestionBlank_throwsBusinessException() {
            given(resilientAiClient.chat(any()))
                    .willReturn(interrogationChatResponseWithQuestion(null, "model"),
                            interrogationChatResponseWithQuestion("", "model"));

            assertThatThrownBy(() -> generator.generateInterrogation(
                    1L, state, List.of(), "Redis", "topic", 1, 4, "answer", 0))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo("AI_007"));
            then(resilientAiClient).should(times(2)).chat(any());
        }

        @Test
        @DisplayName("playground responder: question 1차+2차 모두 blank 여도 throw 하지 않고 결과를 그대로 반환한다 (modeHandler 의 shouldSwitch 평가 책임)")
        void responder_bothQuestionBlank_doesNotThrow() {
            given(resilientAiClient.chat(any()))
                    .willReturn(responderChatResponseWithQuestion("", "정상 가이드", true),
                            responderChatResponseWithQuestion(null, "정상 가이드", true));

            PlaygroundResponderResult actual = generator.generatePlaygroundResponder(
                    1L, state, List.of(), project, "answer", List.of(), 0, 0);

            assertThat(actual.modelAnswer()).isEqualTo("정상 가이드");
            assertThat(actual.question()).isNull();
            assertThat(actual.shouldSwitchToInterrogation()).isTrue();
            then(resilientAiClient).should(times(2)).chat(any());
        }
    }

    private static ChatResponse openerChatResponse(String modelAnswer) {
        return openerChatResponseWithQuestion("Redis 프로젝트 소개해주세요", modelAnswer);
    }

    private static ChatResponse openerChatResponseWithQuestion(String question, String modelAnswer) {
        String json = """
                {
                  "question": %s,
                  "tts_question": "Redis 프로젝트 소개해 주세요",
                  "reason": "오프너",
                  "model_answer": %s
                }
                """.formatted(jsonValue(question), jsonValue(modelAnswer));
        return chatResponse(json);
    }

    private static ChatResponse responderChatResponse(String modelAnswer, boolean shouldSwitch) {
        return responderChatResponseWithQuestion("그 결정의 근거가 뭔가요", modelAnswer, shouldSwitch);
    }

    private static ChatResponse responderChatResponseWithQuestion(
            String question, String modelAnswer, boolean shouldSwitch) {
        String json = """
                {
                  "question": %s,
                  "tts_question": "그 결정의 근거가 뭔가요",
                  "reason": "근거 추적",
                  "should_switch_to_interrogation": %s,
                  "switch_conditions_met": {"a_covered": false, "b_length_ok": false, "c_signal": false, "d_turn_limit": false},
                  "model_answer": %s
                }
                """.formatted(jsonValue(question), shouldSwitch, jsonValue(modelAnswer));
        return chatResponse(json);
    }

    private static ChatResponse interrogationChatResponse(String modelAnswer, String nextAction, int nextLevel) {
        return interrogationChatResponseFull("L2 질문", modelAnswer, nextAction, nextLevel);
    }

    private static ChatResponse interrogationChatResponseWithQuestion(String question, String modelAnswer) {
        return interrogationChatResponseFull(question, modelAnswer, "LEVEL_STAY", 1);
    }

    private static ChatResponse interrogationChatResponseFull(
            String question, String modelAnswer, String nextAction, int nextLevel) {
        String json = """
                {
                  "question": %s,
                  "tts_question": "L2 질문",
                  "reason": "Why-Mech 단계 진입",
                  "next_action": "%s",
                  "next_level": %d,
                  "model_answer": %s
                }
                """.formatted(jsonValue(question), nextAction, nextLevel, jsonValue(modelAnswer));
        return chatResponse(json);
    }

    private static String jsonValue(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }

    private static ChatResponse chatResponse(String content) {
        return new ChatResponse(content, ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
    }
}
