package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.AnswerFeedbackPerspective;
import com.rehearse.api.domain.interview.entity.Claim;
import com.rehearse.api.domain.interview.entity.EvidenceStrength;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowUpSkipHandler - Skip 응답 + 카운터 + 이벤트 발행")
class FollowUpSkipHandlerTest {

    @Mock
    private AiCallMetrics aiCallMetrics;

    @Mock
    private FollowUpTransactionHandler followUpTransactionHandler;

    private FollowUpSkipHandler handler;

    @BeforeEach
    void setUp() {
        handler = new FollowUpSkipHandler(aiCallMetrics, followUpTransactionHandler);
    }

    private static FollowUpContext context() {
        return new FollowUpContext(
                Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 50L, 1,
                ReferenceType.MODEL_ANSWER, 2);
    }

    private static TurnAnalysisResult turn(String text) {
        AnswerAnalysis analysis = new AnswerAnalysis(
                50L,
                List.of(new Claim("c", 3, EvidenceStrength.WEAK, "topic")),
                List.of(AnswerFeedbackPerspective.RELIABILITY),
                List.of(),
                3,
                RecommendedNextAction.SKIP);
        return new TurnAnalysisResult(text, analysis);
    }

    private static FollowUpRequest requestWithExchanges(int previousExchangeCount) {
        FollowUpRequest r = new FollowUpRequest();
        ReflectionTestUtils.setField(r, "questionSetId", 10L);
        if (previousExchangeCount > 0) {
            ReflectionTestUtils.setField(r, "previousExchanges",
                    java.util.Collections.nCopies(previousExchangeCount, null));
        }
        return r;
    }

    @Nested
    @DisplayName("handleAnalyzerSkip")
    class HandleAnalyzerSkip {

        @Test
        @DisplayName("analyzer_recommend_skip 응답 + analyzer_skip 카운터 증가")
        void returnsAnalyzerSkipResponseAndIncrements() {
            FollowUpResponse response = handler.handleAnalyzerSkip(
                    1L, context(), requestWithExchanges(0), turn("답변"));

            assertThat(response.isSkip()).isTrue();
            assertThat(response.getSkipReason()).isEqualTo("analyzer_recommend_skip");
            assertThat(response.getAnswerText()).isEqualTo("답변");
            then(aiCallMetrics).should().incrementFollowUpSkip("analyzer_skip");
        }

        @Test
        @DisplayName("turnIndex 는 previousExchanges 크기 (없으면 0)")
        void publishesEventWithTurnIndexZero_whenNoExchanges() {
            handler.handleAnalyzerSkip(1L, context(), requestWithExchanges(0), turn("답변"));

            then(followUpTransactionHandler).should()
                    .publishTurnCompletedEvent(eq(1L), any(FollowUpContext.class), any(TurnAnalysisResult.class), eq(50L), eq(0));
        }

        @Test
        @DisplayName("previousExchanges 가 N개면 turnIndex=N 으로 이벤트 발행")
        void publishesEventWithTurnIndexFromExchanges() {
            handler.handleAnalyzerSkip(1L, context(), requestWithExchanges(2), turn("답변"));

            then(followUpTransactionHandler).should()
                    .publishTurnCompletedEvent(eq(1L), any(FollowUpContext.class), any(TurnAnalysisResult.class), eq(50L), eq(2));
        }
    }

    @Nested
    @DisplayName("handleStepBSkip")
    class HandleStepBSkip {

        @Test
        @DisplayName("skipReason 그대로 응답 + step_b_skip 카운터 증가")
        void returnsStepBSkipResponseAndIncrements() {
            FollowUpResponse response = handler.handleStepBSkip(
                    1L, context(), requestWithExchanges(1), turn("답변"), "답변이 main_question 과 무관");

            assertThat(response.isSkip()).isTrue();
            assertThat(response.getSkipReason()).isEqualTo("답변이 main_question 과 무관");
            then(aiCallMetrics).should().incrementFollowUpSkip("step_b_skip");
        }

        @Test
        @DisplayName("base questionId 와 turnIndex 로 TurnCompletedEvent 발행")
        void publishesEventWithTurnIndexFromExchanges() {
            handler.handleStepBSkip(1L, context(), requestWithExchanges(3), turn("답변"), "reason");

            then(followUpTransactionHandler).should()
                    .publishTurnCompletedEvent(eq(1L), any(FollowUpContext.class), any(TurnAnalysisResult.class), eq(50L), eq(3));
        }
    }
}
