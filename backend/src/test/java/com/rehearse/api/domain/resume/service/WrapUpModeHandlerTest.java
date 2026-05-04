package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.prompt.ResumeWrapUpPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumeWrapUpPromptBuilder.WrapUpResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WrapUpModeHandler - WRAP_UP 모드 처리")
class WrapUpModeHandlerTest {

    @InjectMocks
    private WrapUpModeHandler handler;

    @Mock
    private ResumeWrapUpPromptBuilder promptBuilder;

    @Mock
    private ResumeQuestionPersister questionPersister;

    private InterviewRuntimeState state;

    @BeforeEach
    void setUp() {
        state = new InterviewRuntimeState("JUNIOR", null);
        Mockito.lenient()
                .when(questionPersister.persist(anyLong(), any(), any(), anyInt()))
                .thenReturn(1L);
    }

    @Nested
    @DisplayName("회고 질문 생성")
    class RetrospectiveQuestion {

        @Test
        @DisplayName("isRetrospective=true 로 호출 시 회고 질문을 포함한 응답을 반환한다")
        void handle_retrospective_returnsWrapUpResponse() {
            given(promptBuilder.build(any(), any(), any(), any(), anyLong(), anyBoolean()))
                    .willReturn(new WrapUpResult("가장 어려웠던 부분이 뭐였나요?", "가장 어려웠던 부분이 뭐였나요?", "이유", true, false));

            WrapUpModeHandler.WrapUpTurnResult result = handler.handle(1L, state, "답변", createAnalysis(), 3L, true, java.util.List.of());

            assertThat(result.response().getQuestion()).isEqualTo("가장 어려웠던 부분이 뭐였나요?");
            assertThat(result.response().getType()).isEqualTo("RESUME_WRAP_UP");
            assertThat(result.response().isSkip()).isFalse();
            assertThat(result.response().isPresentToUser()).isTrue();
        }

        @Test
        @DisplayName("sessionComplete=true 이면 followUpExhausted=true 를 반환한다")
        void handle_sessionComplete_exhausted() {
            given(promptBuilder.build(any(), any(), any(), any(), anyLong(), anyBoolean()))
                    .willReturn(new WrapUpResult("마지막 한 마디", "마지막 한 마디", "이유", true, true));

            WrapUpModeHandler.WrapUpTurnResult result = handler.handle(1L, state, "답변", createAnalysis(), 1L, true, java.util.List.of());

            assertThat(result.response().isFollowUpExhausted()).isTrue();
        }

        @Test
        @DisplayName("remainingMinutes=0 이면 followUpExhausted=true 를 반환한다 — hard timeout")
        void handle_zeroRemaining_exhausted() {
            given(promptBuilder.build(any(), any(), any(), any(), anyLong(), anyBoolean()))
                    .willReturn(new WrapUpResult("질문", "질문", "이유", true, false));

            WrapUpModeHandler.WrapUpTurnResult result = handler.handle(1L, state, "답변", createAnalysis(), 0L, true, java.util.List.of());

            assertThat(result.response().isFollowUpExhausted()).isTrue();
        }

        @Test
        @DisplayName("새 chain/LEVEL_UP/CHAIN_SWITCH 는 발생하지 않는다 — 응답 타입이 RESUME_WRAP_UP 이다")
        void handle_doesNotStartNewChain_typeIsWrapUp() {
            given(promptBuilder.build(any(), any(), any(), any(), anyLong(), anyBoolean()))
                    .willReturn(new WrapUpResult("마무리 질문", "마무리 질문", "이유", true, false));

            WrapUpModeHandler.WrapUpTurnResult result = handler.handle(1L, state, "답변", createAnalysis(), 2L, true, java.util.List.of());

            assertThat(result.response().getType()).isEqualTo("RESUME_WRAP_UP");
            assertThat(state.getChainStateTracker().hasActiveChain()).isFalse();
        }
    }

    @Nested
    @DisplayName("LLM 응답 검증")
    class LlmResponseValidation {

        @Test
        @DisplayName("LLM 이 빈 question 을 반환하면 BusinessException(RESPONSE_INVALID) 을 던진다")
        void handle_blankQuestion_throwsBusinessException() {
            given(promptBuilder.build(any(), any(), any(), any(), anyLong(), anyBoolean()))
                    .willReturn(new WrapUpResult("", "", "이유", true, false));

            assertThatThrownBy(() -> handler.handle(1L, state, "답변", createAnalysis(), 3L, true, java.util.List.of()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo("AI_007"));
        }

        @Test
        @DisplayName("LLM 이 null question 을 반환하면 BusinessException(RESPONSE_INVALID) 을 던진다")
        void handle_nullQuestion_throwsBusinessException() {
            given(promptBuilder.build(any(), any(), any(), any(), anyLong(), anyBoolean()))
                    .willReturn(new WrapUpResult(null, null, "이유", true, false));

            assertThatThrownBy(() -> handler.handle(1L, state, "답변", createAnalysis(), 3L, true, java.util.List.of()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo("AI_007"));
        }
    }

    private AnswerAnalysis createAnalysis() {
        return new AnswerAnalysis(1L, List.of(), List.of(), List.of(), 3, RecommendedNextAction.DEEP_DIVE);
    }
}
