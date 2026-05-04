package com.rehearse.api.infra.ai.context.layer;

import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.FocusHints;
import com.rehearse.api.infra.ai.context.token.TokenEstimator;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FocusLayerTest {

    private FocusLayer focusLayer;

    @BeforeEach
    void setUp() {
        focusLayer = new FocusLayer(new TokenEstimator());
    }

    @Test
    @DisplayName("intent_classifier: mainQuestion + userUtterance 가 프래그먼트에 포함된다")
    void intent_classifier_renders_expected_fragment_when_hints_present() {
        FocusHints hints = new FocusHints.IntentClassifierHints(
                "JVM 메모리 구조를 설명하세요.",
                "힙과 스택으로 나뉘는데요..."
        );

        List<ChatMessage> result = focusLayer.build(req("intent_classifier", hints));

        assertThat(result).hasSize(1);
        String content = result.get(0).content();
        assertThat(content).contains("JVM 메모리 구조를 설명하세요.");
        assertThat(content).contains("힙과 스택으로 나뉘는데요...");
        assertThat(content).contains("의도를 분류하세요");
    }

    @Test
    @DisplayName("intent_classifier: 프래그먼트가 300 토큰을 초과하면 IllegalStateException")
    void intent_classifier_throws_when_fragment_exceeds_cap() {
        String oversized = "A".repeat(FocusLayer.CAP_INTENT_CLASSIFIER * 4 + 100);
        FocusHints hints = new FocusHints.IntentClassifierHints("", oversized);

        assertThatThrownBy(() -> focusLayer.build(req("intent_classifier", hints)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("L4 fragment exceeds " + FocusLayer.CAP_INTENT_CLASSIFIER);
    }

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
    @DisplayName("answer_analyzer: 프래그먼트가 800 토큰을 초과하면 IllegalStateException")
    void answer_analyzer_throws_when_fragment_exceeds_cap() {
        String oversized = "B".repeat(FocusLayer.CAP_ANSWER_ANALYZER * 4 + 100);
        FocusHints hints = new FocusHints.AnswerAnalyzerHints("", oversized, "");

        assertThatThrownBy(() -> focusLayer.build(req("answer_analyzer", hints)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("L4 fragment exceeds " + FocusLayer.CAP_ANSWER_ANALYZER);
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
    @DisplayName("follow_up_generator_v3: 프래그먼트가 1000 토큰을 초과하면 IllegalStateException")
    void follow_up_generator_v3_throws_when_fragment_exceeds_cap() {
        String oversized = "C".repeat(FocusLayer.CAP_FOLLOW_UP_GENERATOR_V3 * 4 + 100);
        FocusHints hints = new FocusHints.FollowUpGeneratorV3Hints(oversized, List.of());

        assertThatThrownBy(() -> focusLayer.build(req("follow_up_generator_v3", hints)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("L4 fragment exceeds " + FocusLayer.CAP_FOLLOW_UP_GENERATOR_V3);
    }

    @Test
    @DisplayName("clarify_response: mainQuestion + userUtterance 가 프래그먼트에 포함된다")
    void clarify_response_renders_expected_fragment_when_hints_present() {
        FocusHints hints = new FocusHints.ClarifyResponseHints(
                "SOLID 원칙이 무엇인가요?", "잘 모르겠어요."
        );

        List<ChatMessage> result = focusLayer.build(req("clarify_response", hints));

        assertThat(result).hasSize(1);
        String content = result.get(0).content();
        assertThat(content).contains("SOLID 원칙이 무엇인가요?");
        assertThat(content).contains("잘 모르겠어요.");
        assertThat(content).contains("재설명");
    }

    @Test
    @DisplayName("clarify_response: 프래그먼트가 400 토큰을 초과하면 IllegalStateException")
    void clarify_response_throws_when_fragment_exceeds_cap() {
        String oversized = "D".repeat(FocusLayer.CAP_CLARIFY_RESPONSE * 4 + 100);
        FocusHints hints = new FocusHints.ClarifyResponseHints("", oversized);

        assertThatThrownBy(() -> focusLayer.build(req("clarify_response", hints)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("L4 fragment exceeds " + FocusLayer.CAP_CLARIFY_RESPONSE);
    }

    @Test
    @DisplayName("giveup_response: mainQuestion + userUtterance + personaDepthHint 가 프래그먼트에 포함된다")
    void giveup_response_renders_expected_fragment_when_hints_present() {
        FocusHints hints = new FocusHints.GiveUpResponseHints(
                "REST와 GraphQL 차이를 설명하세요.", "모르겠습니다.", "GENTLE"
        );

        List<ChatMessage> result = focusLayer.build(req("giveup_response", hints));

        assertThat(result).hasSize(1);
        String content = result.get(0).content();
        assertThat(content).contains("REST와 GraphQL 차이를 설명하세요.");
        assertThat(content).contains("모르겠습니다.");
        assertThat(content).contains("GENTLE");
        assertThat(content).contains("포기");
    }

    @Test
    @DisplayName("giveup_response: 프래그먼트가 400 토큰을 초과하면 IllegalStateException")
    void giveup_response_throws_when_fragment_exceeds_cap() {
        String oversized = "E".repeat(FocusLayer.CAP_GIVEUP_RESPONSE * 4 + 100);
        FocusHints hints = new FocusHints.GiveUpResponseHints("", oversized, "");

        assertThatThrownBy(() -> focusLayer.build(req("giveup_response", hints)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("L4 fragment exceeds " + FocusLayer.CAP_GIVEUP_RESPONSE);
    }

    @Test
    @DisplayName("compaction_summarizer: EmptyHints + 해당 callType 일 때 빈 리스트 반환")
    void compaction_summarizer_returns_empty_list() {
        List<ChatMessage> result = focusLayer.build(req("compaction_summarizer", FocusHints.EmptyHints.INSTANCE));
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("EmptyHints + 등록되지 않은 callType 은 IllegalStateException 으로 fail-fast 한다")
    void unknown_call_type_throws_illegal_state() {
        assertThatThrownBy(() -> focusLayer.build(req("unknown_future_type", FocusHints.EmptyHints.INSTANCE)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("L4 unregistered callType: unknown_future_type");
    }

    @Test
    @DisplayName("L4 출력은 USER 메시지이며 cacheControl=false 이어야 한다")
    void outputs_non_cached_user_message() {
        List<ChatMessage> result = focusLayer.build(
                req("intent_classifier", new FocusHints.IntentClassifierHints("q", "a")));

        assertThat(result).hasSize(1);
        ChatMessage msg = result.get(0);
        assertThat(msg.role()).isEqualTo(ChatMessage.Role.USER);
        assertThat(msg.cacheControl()).isFalse();
    }

    private static ContextBuildRequest req(String callType, FocusHints focusHints) {
        return new ContextBuildRequest(callType, null, null, focusHints, null);
    }
}
