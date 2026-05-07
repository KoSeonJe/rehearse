package com.rehearse.api.infra.ai.context.layer;

import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.FocusHints;
import com.rehearse.api.infra.ai.context.token.TokenEstimator;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FocusLayerTest {

    private FocusLayer focusLayer;
    private TokenEstimator tokenEstimator;

    @BeforeEach
    void setUp() {
        tokenEstimator = new TokenEstimator();
        focusLayer = new FocusLayer(tokenEstimator);
    }

    @Nested
    @DisplayName("정상 렌더링 회귀")
    class NormalRender {

        @Test
        @DisplayName("answer_analyzer: mainQuestion + userAnswer + personaDepthHint 가 프래그먼트에 포함된다")
        void answer_analyzer_renders_expected_fragment_when_hints_present() {
            FocusHints hints = new FocusHints.AnswerAnalyzerHints(
                    "트랜잭션 격리 수준을 설명하세요.",
                    "READ_COMMITTED 는 더티 리드를 방지합니다.",
                    "MID_SENIOR"
            );

            List<ChatMessage> result = focusLayer.build(req("answer_analyzer", hints));

            assertThat(result).hasSize(1);
            String content = result.get(0).content();
            assertThat(content).contains("트랜잭션 격리 수준을 설명하세요.");
            assertThat(content).contains("READ_COMMITTED 는 더티 리드를 방지합니다.");
            assertThat(content).contains("MID_SENIOR");
            assertThat(content).contains("JSON");
        }

        @Test
        @DisplayName("follow_up_generator_v3: answerAnalysisJson + askedPerspectives 가 프래그먼트에 포함된다")
        void follow_up_generator_v3_renders_expected_fragment_when_hints_present() {
            String analysisJson = "{\"claims\":[],\"missing_perspectives\":[\"TRADEOFF\"],\"recommended_next_action\":\"DRILL_DOWN\"}";
            FocusHints hints = new FocusHints.FollowUpGeneratorV3Hints(
                    analysisJson,
                    List.of("RELIABILITY", "SCALABILITY")
            );

            List<ChatMessage> result = focusLayer.build(req("follow_up_generator_v3", hints));

            assertThat(result).hasSize(1);
            String content = result.get(0).content();
            assertThat(content).contains("ANSWER_ANALYSIS:");
            assertThat(content).contains("DRILL_DOWN");
            assertThat(content).contains("RELIABILITY");
            assertThat(content).contains("SCALABILITY");
            assertThat(content).contains("후속 질문을 생성하세요");
        }

        @Test
        @DisplayName("resume_playground_opener: projectInfo + openerQuestion 이 프래그먼트에 포함된다")
        void resume_playground_opener_renders_expected_fragment_when_hints_present() {
            FocusHints hints = new FocusHints.ResumePlaygroundOpenerHints(
                    "결제 시스템 백엔드", "왜 이 프로젝트를 선택했나요?"
            );

            List<ChatMessage> result = focusLayer.build(req("resume_playground_opener", hints));

            assertThat(result).hasSize(1);
            String content = result.get(0).content();
            assertThat(content).contains("결제 시스템 백엔드");
            assertThat(content).contains("왜 이 프로젝트를 선택했나요?");
            assertThat(content).contains("Playground");
        }

        @Test
        @DisplayName("resume_playground_responder: projectInfo + expectedClaims + userAnswer 가 프래그먼트에 포함된다")
        void resume_playground_responder_renders_expected_fragment_when_hints_present() {
            FocusHints hints = new FocusHints.ResumePlaygroundResponderHints(
                    "projectId: p1\nprojectName: 결제 시스템\nclaims: 3개\nimplicitCsTopics: 2개",
                    "동시성 제어 경험", "낙관락으로 처리했습니다.", 3, 240
            );

            List<ChatMessage> result = focusLayer.build(req("resume_playground_responder", hints));

            assertThat(result).hasSize(1);
            String content = result.get(0).content();
            assertThat(content).contains("<<<PROJECT_INFO>>>");
            assertThat(content).contains("projectName: 결제 시스템");
            assertThat(content).contains("동시성 제어 경험");
            assertThat(content).contains("낙관락으로 처리했습니다.");
            assertThat(content).contains("PLAYGROUND_TURN_COUNT: 3");
            assertThat(content).contains("CUMULATIVE_UTTERANCE_LENGTH: 240");
        }

        @Test
        @DisplayName("resume_chain_interrogator: projectName + chain 상태와 userAnswer 가 프래그먼트에 포함된다")
        void resume_chain_interrogator_renders_expected_fragment_when_hints_present() {
            FocusHints hints = new FocusHints.ResumeChainInterrogatorHints(
                    "결제 시스템 백엔드", "동시성 chain", 2, 1, "낙관락 사용", 1
            );

            List<ChatMessage> result = focusLayer.build(req("resume_chain_interrogator", hints));

            assertThat(result).hasSize(1);
            String content = result.get(0).content();
            assertThat(content).contains("<<<PROJECT_NAME>>>");
            assertThat(content).contains("결제 시스템 백엔드");
            assertThat(content).contains("동시성 chain");
            assertThat(content).contains("CURRENT_LEVEL: 2");
            assertThat(content).contains("ANSWER_QUALITY: 1");
            assertThat(content).contains("CONSECUTIVE_STAY_COUNT: 1");
            assertThat(content).contains("낙관락 사용");
        }

        @Test
        @DisplayName("resume_wrap_up: sessionSummary + remainingMinutes 가 프래그먼트에 포함되며 PROJECT_NAME 블록은 emit 되지 않는다")
        void resume_wrap_up_renders_expected_fragment_when_hints_present() {
            FocusHints hints = new FocusHints.ResumeWrapUpHints(
                    "세션 요약본", 5, true
            );

            List<ChatMessage> result = focusLayer.build(req("resume_wrap_up", hints));

            assertThat(result).hasSize(1);
            String content = result.get(0).content();
            assertThat(content).doesNotContain("<<<PROJECT_NAME>>>");
            assertThat(content).contains("세션 요약본");
            assertThat(content).contains("REMAINING_MINUTES: 5");
            assertThat(content).contains("IS_RETROSPECTIVE: true");
            assertThat(content).contains("WRAP_UP");
        }

        @Test
        @DisplayName("L4 출력은 USER 메시지이며 cacheControl=false 이어야 한다")
        void outputs_non_cached_user_message() {
            List<ChatMessage> result = focusLayer.build(
                    req("answer_analyzer",
                            new FocusHints.AnswerAnalyzerHints("질문", "답변", "MID_SENIOR")));

            assertThat(result).hasSize(1);
            ChatMessage msg = result.get(0);
            assertThat(msg.role()).isEqualTo(ChatMessage.Role.USER);
            assertThat(msg.cacheControl()).isFalse();
        }
    }

    @Nested
    @DisplayName("cap 초과 시 본문 절단으로 정상 반환된다 (P0-1)")
    class CapExceededTruncate {

        @Test
        @DisplayName("answer_analyzer: cap 초과 시 절단 후 지시문 (JSON 응답 안내) 보존")
        void answer_analyzer_truncates_preserves_instruction_tail() {
            String oversized = "B".repeat(FocusLayer.CAP_ANSWER_ANALYZER * 4 + 500);
            FocusHints hints = new FocusHints.AnswerAnalyzerHints("질문", oversized, "MID_SENIOR");

            List<ChatMessage> result = focusLayer.build(req("answer_analyzer", hints));

            assertThat(result).hasSize(1);
            String content = result.get(0).content();
            assertThat(tokenEstimator.estimate(content))
                    .isLessThanOrEqualTo((int) Math.ceil(FocusLayer.CAP_ANSWER_ANALYZER * 0.9) + 1);
            assertThat(content).contains("위 답변을 분석해");
            assertThat(content).endsWith("JSON 한 객체로만 응답하세요.");
        }

        @Test
        @DisplayName("resume_chain_interrogator: cap 초과 시 절단 후 LEVEL/QUALITY 지시문 보존")
        void resume_chain_interrogator_truncates_preserves_instruction_tail() {
            String oversized = "C".repeat(FocusLayer.CAP_RESUME_CHAIN_INTERROGATOR * 4 + 800);
            FocusHints hints = new FocusHints.ResumeChainInterrogatorHints(
                    "결제", "chain", 2, 1, oversized, 1);

            List<ChatMessage> result = focusLayer.build(req("resume_chain_interrogator", hints));

            assertThat(result).hasSize(1);
            String content = result.get(0).content();
            assertThat(tokenEstimator.estimate(content))
                    .isLessThanOrEqualTo((int) Math.ceil(FocusLayer.CAP_RESUME_CHAIN_INTERROGATOR * 0.9) + 1);
            assertThat(content).contains("CURRENT_LEVEL: 2");
            assertThat(content).contains("LEVEL_UP/LEVEL_STAY/CHAIN_SWITCH");
            assertThat(content).endsWith("JSON 한 객체로만 응답하세요.");
        }
    }

    @Nested
    @DisplayName("EmptyHints 분기 — compaction_summarizer / 미등록 callType (P0-2)")
    class EmptyHintsHandling {

        @Test
        @DisplayName("compaction_summarizer: EmptyHints 일 때 빈 리스트 반환 (회귀)")
        void compaction_summarizer_returns_empty_list() {
            List<ChatMessage> result = focusLayer.build(
                    req("compaction_summarizer", FocusHints.EmptyHints.INSTANCE));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("미등록 callType 은 빈 리스트로 graceful 폴백 (throw 하지 않는다)")
        void unknown_call_type_returns_empty_list_without_throw() {
            List<ChatMessage> result = focusLayer.build(
                    req("unknown_future_type", FocusHints.EmptyHints.INSTANCE));
            assertThat(result).isEmpty();
        }
    }

    private static ContextBuildRequest req(String callType, FocusHints focusHints) {
        return new ContextBuildRequest(callType, null, null, focusHints, null);
    }
}
