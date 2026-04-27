package com.rehearse.api.infra.ai.context.layer;

import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.token.TokenEstimator;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FixedContextLayerTest {

    private FixedContextLayer layer;
    private TokenEstimator estimator;

    @BeforeEach
    void setUp() {
        layer = new FixedContextLayer();
        layer.init();
        estimator = new TokenEstimator();
    }

    @Test
    @DisplayName("L1은 단일 SYSTEM 메시지를 반환한다")
    void build_returnsSingleSystemMessage_whenAnyCallType() {
        ContextBuildRequest req = new ContextBuildRequest(
                "intent_classifier", null, null, null, null);

        List<ChatMessage> result = layer.build(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).role()).isEqualTo(ChatMessage.Role.SYSTEM);
    }

    @Test
    @DisplayName("L1 SYSTEM 메시지는 cacheControl=true 로 마킹된다")
    void build_returnsCacheControlTrue_whenAnyCallType() {
        ContextBuildRequest req = new ContextBuildRequest(
                "follow_up_generator_v3", null, null, null, null);

        List<ChatMessage> result = layer.build(req);

        assertThat(result.get(0).cacheControl()).isTrue();
    }

    @Test
    @DisplayName("intent_classifier callType은 intent_classifier skeleton을 포함한다")
    void build_containsIntentClassifierSkeleton_whenIntentClassifierCallType() {
        ContextBuildRequest req = new ContextBuildRequest(
                "intent_classifier", null, null, null, null);

        List<ChatMessage> result = layer.build(req);
        String content = result.get(0).content();

        assertThat(content).contains("분류기");
        assertThat(content).contains("ANSWER");
        assertThat(content).contains("CLARIFY_REQUEST");
        assertThat(content).contains("GIVE_UP");
        assertThat(content).contains("OFF_TOPIC");
    }

    @Test
    @DisplayName("follow_up_generator_v3 callType은 꼬리질문 생성기 skeleton을 포함한다")
    void build_containsFollowUpGeneratorSkeleton_whenFollowUpGeneratorV3CallType() {
        ContextBuildRequest req = new ContextBuildRequest(
                "follow_up_generator_v3", null, null, null, null);

        List<ChatMessage> result = layer.build(req);
        String content = result.get(0).content();

        assertThat(content).contains("꼬리질문");
        assertThat(content).contains("DEEP_DIVE");
        assertThat(content).contains("CLARIFICATION");
    }

    @Test
    @DisplayName("answer_analyzer callType은 분석기 skeleton을 포함한다")
    void build_containsAnswerAnalyzerSkeleton_whenAnswerAnalyzerCallType() {
        ContextBuildRequest req = new ContextBuildRequest(
                "answer_analyzer", null, null, null, null);

        List<ChatMessage> result = layer.build(req);
        String content = result.get(0).content();

        assertThat(content).contains("분석기");
        assertThat(content).contains("claims");
        assertThat(content).contains("recommended_next_action");
    }

    @Test
    @DisplayName("intent_classifier와 follow_up_generator_v3는 서로 다른 skeleton tail을 가진다")
    void build_producesDifferentSkeletonTails_whenDifferentCallTypes() {
        ContextBuildRequest intentReq = new ContextBuildRequest(
                "intent_classifier", null, null, null, null);
        ContextBuildRequest followUpReq = new ContextBuildRequest(
                "follow_up_generator_v3", null, null, null, null);

        String intentContent = layer.build(intentReq).get(0).content();
        String followUpContent = layer.build(followUpReq).get(0).content();

        assertThat(intentContent).isNotEqualTo(followUpContent);
    }

    @Test
    @DisplayName("모든 callType의 L1 블록은 공통 보안 규칙을 포함한다")
    void build_containsGlobalSecurityRules_forAllCallTypes() {
        List<String> callTypes = List.of(
                "intent_classifier", "answer_analyzer",
                "follow_up_generator_v3", "clarify_response", "giveup_response"
        );

        for (String callType : callTypes) {
            ContextBuildRequest req = new ContextBuildRequest(
                    callType, null, null, null, null);
            String content = layer.build(req).get(0).content();

            assertThat(content)
                    .as("callType=%s 에서 보안 규칙 누락", callType)
                    .contains("보안 규칙");
            assertThat(content)
                    .as("callType=%s 에서 구분자 규칙 누락", callType)
                    .contains("구분자 규칙");
        }
    }

    @Test
    @DisplayName("L1 토큰 추정값은 callType별로 3000~5000 범위 안에 있다")
    void build_tokenEstimateWithinTarget_forKnownCallTypes() {
        List<String> callTypes = List.of(
                "intent_classifier", "answer_analyzer",
                "follow_up_generator_v3", "clarify_response", "giveup_response"
        );

        for (String callType : callTypes) {
            ContextBuildRequest req = new ContextBuildRequest(
                    callType, null, null, null, null);
            List<ChatMessage> messages = layer.build(req);
            int tokens = estimator.estimate(messages);

            assertThat(tokens)
                    .as("callType=%s token estimate=%d should be within 50-5000", callType, tokens)
                    .isBetween(50, 5000);
        }
    }

    @Test
    @DisplayName("알 수 없는 callType은 default skeleton을 사용하며 공통 코어를 포함한다")
    void build_usesDefaultSkeleton_whenUnknownCallType() {
        ContextBuildRequest req = new ContextBuildRequest(
                "unknown_call_type", null, null, null, null);

        List<ChatMessage> result = layer.build(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).contains("보안 규칙");
    }
}
