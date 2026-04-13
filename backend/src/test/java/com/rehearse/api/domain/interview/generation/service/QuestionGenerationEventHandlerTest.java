package com.rehearse.api.domain.interview.generation.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class QuestionGenerationEventHandlerTest {

    @InjectMocks
    private QuestionGenerationEventHandler eventHandler;

    @Mock
    private QuestionGenerationService questionGenerationService;

    @Mock
    private QuestionGenerationTransactionHandler transactionHandler;

    private QuestionGenerationRequestedEvent createEvent(Long interviewId, Long userId) {
        return new QuestionGenerationRequestedEvent(
                interviewId, userId,
                Position.BACKEND, null,
                InterviewLevel.JUNIOR,
                List.of(InterviewType.CS_FUNDAMENTAL),
                List.of(),
                null, 30,
                TechStack.JAVA_SPRING);
    }

    @Nested
    @DisplayName("handleQuestionGenerationEvent - 정상 처리")
    class NormalProcessing {

        @Test
        @DisplayName("정상 이벤트 수신 시 generateQuestions가 호출된다")
        void normalEvent_callsGenerateQuestions() {
            // given
            QuestionGenerationRequestedEvent event = createEvent(1L, 1L);

            // when
            eventHandler.handleQuestionGenerationEvent(event);

            // then
            then(questionGenerationService).should().generateQuestions(
                    eq(1L), eq(1L), eq(Position.BACKEND), eq(InterviewLevel.JUNIOR),
                    eq(List.of(InterviewType.CS_FUNDAMENTAL)), eq(List.of()),
                    isNull(), eq(30), eq(TechStack.JAVA_SPRING));
        }

        @Test
        @DisplayName("이벤트 파라미터가 generateQuestions에 그대로 전달된다")
        void eventParameters_passedToGenerateQuestions() {
            // given
            QuestionGenerationRequestedEvent event = new QuestionGenerationRequestedEvent(
                    42L, 99L,
                    Position.FRONTEND, "React 개발자",
                    InterviewLevel.SENIOR,
                    List.of(InterviewType.CS_FUNDAMENTAL, InterviewType.BEHAVIORAL),
                    List.of("OS", "Network"),
                    "이력서 내용", 45,
                    TechStack.REACT_TS);

            // when
            eventHandler.handleQuestionGenerationEvent(event);

            // then
            then(questionGenerationService).should().generateQuestions(
                    eq(42L), eq(99L), eq(Position.FRONTEND), eq(InterviewLevel.SENIOR),
                    eq(List.of(InterviewType.CS_FUNDAMENTAL, InterviewType.BEHAVIORAL)),
                    eq(List.of("OS", "Network")),
                    eq("이력서 내용"), eq(45), eq(TechStack.REACT_TS));
        }
    }

    @Nested
    @DisplayName("handleQuestionGenerationEvent - 예외 처리")
    class ExceptionHandling {

        @Test
        @DisplayName("서비스 예외 발생 시 failGeneration이 호출된다")
        void serviceThrows_callsFailGeneration() {
            // given
            QuestionGenerationRequestedEvent event = createEvent(1L, 1L);
            RuntimeException cause = new RuntimeException("AI 서비스 오류");
            RuntimeException wrapper = new RuntimeException("래핑된 예외", cause);
            willThrow(wrapper).given(questionGenerationService).generateQuestions(
                    anyLong(), anyLong(), any(), any(), anyList(), anyList(), any(), any(), any());

            // when
            eventHandler.handleQuestionGenerationEvent(event);

            // then
            then(transactionHandler).should().failGeneration(eq(1L), eq("AI 서비스 오류"));
        }

        @Test
        @DisplayName("예외에 cause가 있으면 cause.getMessage()를 failGeneration에 전달한다")
        void exceptionWithCause_usesCauseMessage() {
            // given
            QuestionGenerationRequestedEvent event = createEvent(1L, 1L);
            RuntimeException cause = new RuntimeException("원인 메시지");
            RuntimeException wrapper = new RuntimeException("래퍼 메시지", cause);
            willThrow(wrapper).given(questionGenerationService).generateQuestions(
                    anyLong(), anyLong(), any(), any(), anyList(), anyList(), any(), any(), any());

            // when
            eventHandler.handleQuestionGenerationEvent(event);

            // then: cause.getMessage() = "원인 메시지"
            then(transactionHandler).should().failGeneration(eq(1L), eq("원인 메시지"));
        }

        @Test
        @DisplayName("예외에 cause가 없으면 e.getMessage()를 failGeneration에 전달한다")
        void exceptionWithoutCause_usesExceptionMessage() {
            // given
            QuestionGenerationRequestedEvent event = createEvent(1L, 1L);
            RuntimeException noCauseException = new RuntimeException("직접 오류 메시지");
            willThrow(noCauseException).given(questionGenerationService).generateQuestions(
                    anyLong(), anyLong(), any(), any(), anyList(), anyList(), any(), any(), any());

            // when
            eventHandler.handleQuestionGenerationEvent(event);

            // then: e.getCause() == null → e.getMessage() = "직접 오류 메시지"
            then(transactionHandler).should().failGeneration(eq(1L), eq("직접 오류 메시지"));
        }

        @Test
        @DisplayName("예외 메시지가 null이면 기본 에러 메시지가 사용된다")
        void nullExceptionMessage_usesDefaultMessage() {
            // given
            QuestionGenerationRequestedEvent event = createEvent(1L, 1L);
            RuntimeException nullMessageException = new RuntimeException((String) null);
            willThrow(nullMessageException).given(questionGenerationService).generateQuestions(
                    anyLong(), anyLong(), any(), any(), anyList(), anyList(), any(), any(), any());

            // when
            eventHandler.handleQuestionGenerationEvent(event);

            // then: reason == null → "알 수 없는 오류"
            then(transactionHandler).should().failGeneration(eq(1L), eq("알 수 없는 오류"));
        }

        @Test
        @DisplayName("서비스 예외 발생 시 failGeneration이 반드시 호출된다")
        void serviceThrows_failGenerationAlwaysCalled() {
            // given
            QuestionGenerationRequestedEvent event = createEvent(1L, 1L);
            willThrow(new RuntimeException("서비스 오류")).given(questionGenerationService).generateQuestions(
                    anyLong(), anyLong(), any(), any(), anyList(), anyList(), any(), any(), any());

            // when
            eventHandler.handleQuestionGenerationEvent(event);

            // then: failGeneration이 정확히 한 번 호출된다
            then(transactionHandler).should().failGeneration(eq(1L), anyString());
        }
    }
}
