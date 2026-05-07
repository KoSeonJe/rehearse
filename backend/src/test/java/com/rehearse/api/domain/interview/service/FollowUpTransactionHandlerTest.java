package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.feedback.rubric.event.TurnCompletedEvent;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpSaveResult;
import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.domain.interview.service.InterviewTurnPolicy;
import com.rehearse.api.domain.interview.service.InterviewTurnPolicyResolver;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.domain.question.exception.QuestionErrorCode;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionSetCategory;
import com.rehearse.api.domain.question.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowUpTransactionHandler - 꼬리질문 트랜잭션 처리")
class FollowUpTransactionHandlerTest {

    @InjectMocks
    private FollowUpTransactionHandler handler;

    @Mock
    private InterviewFinder interviewFinder;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private InterviewTurnPolicyResolver turnPolicyResolver;

    @Mock
    private InterviewTurnPolicy turnPolicy;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Nested
    @DisplayName("loadFollowUpContext 메서드")
    class LoadFollowUpContext {

        @Test
        @DisplayName("loadFollowUpContext - 정상: IN_PROGRESS 면접에서 컨텍스트 로드")
        void loadFollowUpContext_success() {
            // given
            Interview interview = createInProgressInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);

            QuestionSet questionSet = createQuestionSetWithMainQuestion(interview, ReferenceType.MODEL_ANSWER);
            given(questionSetRepository.findById(10L)).willReturn(Optional.of(questionSet));
            given(turnPolicyResolver.resolve(interview)).willReturn(turnPolicy);
            given(turnPolicy.getMaxFollowUpRounds()).willReturn(2);

            // when
            FollowUpContext context = handler.loadFollowUpContext(1L, 1L, 10L);

            // then
            assertThat(context.position()).isEqualTo(Position.BACKEND);
            assertThat(context.level()).isEqualTo(InterviewLevel.JUNIOR);
            assertThat(context.questionSetId()).isEqualTo(10L);
            assertThat(context.nextOrderIndex()).isEqualTo(1);
            assertThat(context.mainReferenceType()).isEqualTo(ReferenceType.MODEL_ANSWER);
            assertThat(context.maxFollowUpRounds()).isEqualTo(2);
        }

        @Test
        @DisplayName("loadFollowUpContext - 메인 질문의 referenceType이 GUIDE면 context에 GUIDE가 실린다 (EXPERIENCE 모드)")
        void loadFollowUpContext_resumeQuestion_carriesGuideReferenceType() {
            // given
            Interview interview = createInProgressInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);

            QuestionSet questionSet = createQuestionSetWithMainQuestion(interview, ReferenceType.GUIDE);
            given(questionSetRepository.findById(10L)).willReturn(Optional.of(questionSet));
            given(turnPolicyResolver.resolve(interview)).willReturn(turnPolicy);

            // when
            FollowUpContext context = handler.loadFollowUpContext(1L, 1L, 10L);

            // then
            assertThat(context.mainReferenceType()).isEqualTo(ReferenceType.GUIDE);
        }

        @Test
        @DisplayName("loadFollowUpContext - 메인 질문의 referenceType이 null이면 안전 기본값 MODEL_ANSWER로 폴백")
        void loadFollowUpContext_nullReferenceType_fallsBackToModelAnswer() {
            // given
            Interview interview = createInProgressInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);

            QuestionSet questionSet = createQuestionSetWithMainQuestion(interview, null);
            given(questionSetRepository.findById(10L)).willReturn(Optional.of(questionSet));
            given(turnPolicyResolver.resolve(interview)).willReturn(turnPolicy);

            // when
            FollowUpContext context = handler.loadFollowUpContext(1L, 1L, 10L);

            // then
            assertThat(context.mainReferenceType()).isEqualTo(ReferenceType.MODEL_ANSWER);
        }

        @Test
        @DisplayName("loadFollowUpContext - 예외: 면접이 IN_PROGRESS가 아닌 경우")
        void loadFollowUpContext_notInProgress() {
            // given
            Interview interview = createMockInterview(); // READY 상태
            given(interviewFinder.findById(1L)).willReturn(interview);

            // when & then
            assertThatThrownBy(() -> handler.loadFollowUpContext(1L, 1L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_003");
                    });
        }

        @Test
        @DisplayName("loadFollowUpContext - 예외: QuestionSet 미존재")
        void loadFollowUpContext_questionSetNotFound() {
            // given
            Interview interview = createInProgressInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);
            given(questionSetRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> handler.loadFollowUpContext(1L, 1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("QUESTION_SET_001");
                    });
        }

        @Test
        @DisplayName("loadFollowUpContext - 예외: 턴 정책이 MAX_FOLLOWUP_EXCEEDED 를 throw 하면 그대로 전파")
        void loadFollowUpContext_policyRejects_propagatesException() {
            // given
            Interview interview = createInProgressInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);

            QuestionSet questionSet = createQuestionSetWithFollowUps(interview, 2);
            given(questionSetRepository.findById(10L)).willReturn(Optional.of(questionSet));
            given(turnPolicyResolver.resolve(interview)).willReturn(turnPolicy);
            willThrow(new BusinessException(QuestionErrorCode.MAX_FOLLOWUP_EXCEEDED))
                    .given(turnPolicy).assertCanContinue(interview, questionSet);

            // when & then
            assertThatThrownBy(() -> handler.loadFollowUpContext(1L, 1L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("QUESTION_SET_004");
                    });
        }
    }

    @Nested
    @DisplayName("saveFollowUpResult 메서드")
    class SaveFollowUpResult {

        @Test
        @DisplayName("saveFollowUpResult - 정상: 후속질문 저장")
        void saveFollowUpResult_success() {
            // given
            Interview interview = createInProgressInterview();
            QuestionSet questionSet = createQuestionSetWithMainQuestion(interview, ReferenceType.MODEL_ANSWER);
            given(questionSetRepository.findById(10L)).willReturn(Optional.of(questionSet));

            GeneratedFollowUp followUp = new GeneratedFollowUp();
            ReflectionTestUtils.setField(followUp, "question", "해시 충돌 해결 방법은?");
            ReflectionTestUtils.setField(followUp, "modelAnswer", "체이닝과 오픈 어드레싱");

            Question savedQuestion = Question.builder()
                    .questionType(QuestionType.FOLLOWUP)
                    .questionText("해시 충돌 해결 방법은?")
                    .orderIndex(1)
                    .build();
            ReflectionTestUtils.setField(savedQuestion, "id", 100L);
            given(questionRepository.saveAndFlush(any(Question.class))).willReturn(savedQuestion);

            // when
            FollowUpSaveResult result = handler.saveFollowUpResult(10L, followUp);

            // then
            assertThat(result.question().getId()).isEqualTo(100L);
            assertThat(result.question().getQuestionText()).isEqualTo("해시 충돌 해결 방법은?");
            assertThat(result.newFollowUpCount()).isEqualTo(1);
            then(questionRepository).should().saveAndFlush(any(Question.class));
        }

        @Test
        @DisplayName("saveFollowUpResultAndPublishEvent - 저장과 이벤트 발행을 같은 트랜잭션 메서드에서 처리한다")
        void saveFollowUpResultAndPublishEvent_savesAndPublishes() {
            Interview interview = createInProgressInterview();
            QuestionSet questionSet = createQuestionSetWithMainQuestion(interview, ReferenceType.MODEL_ANSWER);
            given(questionSetRepository.findById(10L)).willReturn(Optional.of(questionSet));
            given(interviewFinder.findById(1L)).willReturn(interview);

            GeneratedFollowUp followUp = new GeneratedFollowUp();
            ReflectionTestUtils.setField(followUp, "question", "해시 충돌 해결 방법은?");

            Question savedQuestion = Question.builder()
                    .questionType(QuestionType.FOLLOWUP)
                    .questionText("해시 충돌 해결 방법은?")
                    .orderIndex(1)
                    .build();
            ReflectionTestUtils.setField(savedQuestion, "id", 100L);
            given(questionRepository.saveAndFlush(any(Question.class))).willReturn(savedQuestion);

            FollowUpSaveResult result = handler.saveFollowUpResultAndPublishEvent(
                    1L, createContext(), followUp, createTurn(IntentType.ANSWER));

            assertThat(result.question().getId()).isEqualTo(100L);
            TurnCompletedEvent event = capturePublishedEvent();
            assertThat(event.questionId()).isEqualTo(100L);
            assertThat(event.questionSetId()).isEqualTo(10L);
            assertThat(event.turnIndex()).isEqualTo(1L);
        }

        @Test
        @DisplayName("publishTurnCompletedEvent - 저장 없는 종료 분기도 짧은 트랜잭션 메서드에서 이벤트를 발행한다")
        void publishTurnCompletedEvent_publishesBaseQuestionEvent() {
            Interview interview = createInProgressInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);

            handler.publishTurnCompletedEvent(
                    1L, createContext(), createTurn(IntentType.CLARIFY_REQUEST), 50L, 0);

            TurnCompletedEvent event = capturePublishedEvent();
            assertThat(event.questionId()).isEqualTo(50L);
            assertThat(event.questionSetId()).isEqualTo(10L);
            assertThat(event.turnIndex()).isZero();
            assertThat(event.intent()).isEqualTo(IntentType.CLARIFY_REQUEST);
        }

        @Test
        @DisplayName("saveFollowUpResult - 기존 followUp 1개가 있던 세트에 추가하면 newFollowUpCount=2")
        void saveFollowUpResult_increments_newFollowUpCount() {
            Interview interview = createInProgressInterview();
            QuestionSet questionSet = createQuestionSetWithFollowUps(interview, 1);
            given(questionSetRepository.findById(10L)).willReturn(Optional.of(questionSet));

            GeneratedFollowUp followUp = new GeneratedFollowUp();
            ReflectionTestUtils.setField(followUp, "question", "두 번째 꼬리질문");

            Question savedQuestion = Question.builder()
                    .questionType(QuestionType.FOLLOWUP).questionText("두 번째 꼬리질문").orderIndex(2).build();
            ReflectionTestUtils.setField(savedQuestion, "id", 200L);
            given(questionRepository.saveAndFlush(any(Question.class))).willReturn(savedQuestion);

            FollowUpSaveResult result = handler.saveFollowUpResult(10L, followUp);

            assertThat(result.newFollowUpCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("saveFollowUpResult - 동시 호출 중복 시 DataIntegrityViolation → FOLLOWUP_DUPLICATE BusinessException")
        void saveFollowUpResult_throws_followup_duplicate_on_unique_constraint_violation() {
            // given
            Interview interview = createInProgressInterview();
            QuestionSet questionSet = createQuestionSetWithMainQuestion(interview, ReferenceType.MODEL_ANSWER);
            given(questionSetRepository.findById(10L)).willReturn(Optional.of(questionSet));

            GeneratedFollowUp followUp = new GeneratedFollowUp();
            ReflectionTestUtils.setField(followUp, "question", "중복 꼬리질문");

            given(questionRepository.saveAndFlush(any(Question.class)))
                    .willThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate entry"));

            // when / then
            assertThatThrownBy(() -> handler.saveFollowUpResult(10L, followUp))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getCode()).isEqualTo(
                                com.rehearse.api.domain.interview.exception.InterviewErrorCode.FOLLOWUP_DUPLICATE.getCode());
                        assertThat(be.getStatus()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
                    });
        }
    }

    private Interview createMockInterview() {
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();
        ReflectionTestUtils.setField(interview, "id", 1L);
        ReflectionTestUtils.setField(interview, "userId", 1L);
        return interview;
    }

    private Interview createInProgressInterview() {
        Interview interview = createMockInterview();
        interview.completeQuestionGeneration();
        interview.updateStatus(InterviewStatus.IN_PROGRESS);
        return interview;
    }

    private QuestionSet createQuestionSetWithMainQuestion(Interview interview, ReferenceType mainReferenceType) {
        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category(QuestionSetCategory.CS_FUNDAMENTAL)
                .orderIndex(0)
                .build();
        ReflectionTestUtils.setField(qs, "id", 10L);

        Question mainQuestion = Question.builder()
                .questionType(QuestionType.MAIN)
                .questionText("HashMap과 TreeMap의 차이점은?")
                .referenceType(mainReferenceType)
                .orderIndex(0)
                .build();
        qs.addQuestion(mainQuestion);
        return qs;
    }

    private QuestionSet createQuestionSetWithFollowUps(Interview interview, int followUpCount) {
        QuestionSet qs = createQuestionSetWithMainQuestion(interview, ReferenceType.MODEL_ANSWER);
        for (int i = 0; i < followUpCount; i++) {
            Question followUp = Question.builder()
                    .questionType(QuestionType.FOLLOWUP)
                    .questionText("후속질문 " + (i + 1))
                    .orderIndex(i + 1)
                    .build();
            qs.addQuestion(followUp);
        }
        return qs;
    }

    private FollowUpContext createContext() {
        return new FollowUpContext(
                Position.BACKEND, TechStack.JAVA_SPRING, InterviewLevel.JUNIOR,
                10L, 50L, 1, ReferenceType.MODEL_ANSWER, 2);
    }

    private TurnAnalysisResult createTurn(IntentType intentType) {
        return new TurnAnalysisResult(
                "답변 텍스트",
                IntentResult.of(intentType, 0.9, "test"),
                new AnswerAnalysis(50L, List.of(), List.of(), List.of(), 3, RecommendedNextAction.DEEP_DIVE));
    }

    private TurnCompletedEvent capturePublishedEvent() {
        ArgumentCaptor<TurnCompletedEvent> captor = ArgumentCaptor.forClass(TurnCompletedEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        return captor.getValue();
    }
}
