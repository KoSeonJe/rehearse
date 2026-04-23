package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.service.CacheableQuestionProvider;
import com.rehearse.api.domain.question.service.FreshQuestionProvider;
import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.service.QuestionGenerationService;
import com.rehearse.api.domain.question.service.QuestionGenerationTransactionHandler;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class QuestionGenerationServiceTest {

    @InjectMocks
    private QuestionGenerationService questionGenerationService;

    @Mock
    private QuestionGenerationTransactionHandler transactionHandler;

    @Mock
    private CacheableQuestionProvider cacheableProvider;

    @Mock
    private FreshQuestionProvider freshProvider;

    // virtualExecutor는 final 필드이므로 실제 virtual thread executor를 그대로 사용.
    // generateQuestions()는 내부에서 .join()으로 블로킹하므로 테스트는 결정론적으로 동작한다.

    private QuestionPool makePool(String content, String referenceType) {
        return QuestionPool.create("key:cs:junior", content, null, null, null, referenceType);
    }

    private GeneratedQuestion makeGeneratedQuestion(String content, String questionCategory, String referenceType) {
        GeneratedQuestion gq = new GeneratedQuestion();
        ReflectionTestUtils.setField(gq, "content", content);
        ReflectionTestUtils.setField(gq, "ttsContent", null);
        ReflectionTestUtils.setField(gq, "questionCategory", questionCategory);
        ReflectionTestUtils.setField(gq, "modelAnswer", null);
        ReflectionTestUtils.setField(gq, "referenceType", referenceType);
        return gq;
    }

    @Nested
    @DisplayName("generateQuestions - Provider 호출 분기")
    class ProviderCallBranch {

        @Test
        @DisplayName("혼합 타입(cacheable+fresh) 요청 시 양쪽 Provider가 모두 호출된다")
        void mixedTypes_callsBothProviders() {
            // given
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL, InterviewType.RESUME_BASED);
            QuestionPool pool = makePool("CS 질문", "MODEL_ANSWER");
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(pool));
            GeneratedQuestion gq = makeGeneratedQuestion("이력서 질문", "RESUME", "GUIDE");
            given(freshProvider.provide(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                    .willReturn(List.of(gq));

            // when
            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), "이력서 내용", 30, TechStack.JAVA_SPRING);

            // then
            then(cacheableProvider).should().provide(anyLong(), any(), any(), any(), any(), anyInt(), any());
            then(freshProvider).should().provide(any(), any(), any(), any(), anyInt(), any(), any(), any());
        }

        @Test
        @DisplayName("cacheable 타입만 있을 때 FreshProvider는 호출되지 않는다")
        void cacheableOnly_doesNotCallFreshProvider() {
            // given
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL);
            QuestionPool pool = makePool("CS 질문", "MODEL_ANSWER");
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(pool));

            // when
            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), null, 30, TechStack.JAVA_SPRING);

            // then
            then(freshProvider).should(never()).provide(any(), any(), any(), any(), anyInt(), any(), any(), any());
        }

        @Test
        @DisplayName("fresh 타입만 있을 때 CacheableProvider는 호출되지 않는다")
        void freshOnly_doesNotCallCacheableProvider() {
            // given
            List<InterviewType> types = List.of(InterviewType.RESUME_BASED);
            GeneratedQuestion gq = makeGeneratedQuestion("이력서 질문", "RESUME", "GUIDE");
            given(freshProvider.provide(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                    .willReturn(List.of(gq));

            // when
            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), "이력서", 30, TechStack.JAVA_SPRING);

            // then
            then(cacheableProvider).should(never()).provide(any(), any(), any(), any(), any(), anyInt(), any());
        }
    }

    @Nested
    @DisplayName("generateQuestions - 트랜잭션 핸들러 호출 순서")
    class TransactionHandlerOrder {

        @Test
        @DisplayName("startGeneration은 Provider 호출보다 먼저 실행된다")
        void startGeneration_calledBeforeProviders() {
            // given
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL);
            QuestionPool pool = makePool("CS 질문", "MODEL_ANSWER");
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), any(), anyInt(), any()))
                    .willReturn(List.of(pool));

            // when
            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), null, 30, TechStack.JAVA_SPRING);

            // then
            org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(transactionHandler, cacheableProvider);
            inOrder.verify(transactionHandler).startGeneration(1L);
            inOrder.verify(cacheableProvider).provide(anyLong(), any(), any(), any(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("saveResults는 Provider 호출 이후에 실행된다")
        void saveResults_calledAfterProviders() {
            // given
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL);
            QuestionPool pool = makePool("CS 질문", "MODEL_ANSWER");
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), any(), anyInt(), any()))
                    .willReturn(List.of(pool));

            // when
            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), null, 30, TechStack.JAVA_SPRING);

            // then
            org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(cacheableProvider, transactionHandler);
            inOrder.verify(cacheableProvider).provide(anyLong(), any(), any(), any(), any(), anyInt(), any());
            inOrder.verify(transactionHandler).saveResults(eq(1L), anyList());
        }
    }

    @Nested
    @DisplayName("generateQuestions - 예외 처리")
    class ExceptionHandling {

        @Test
        @DisplayName("CacheableProvider 예외 발생 시 RuntimeException이 전파된다")
        void cacheableProviderThrows_propagatesRuntimeException() {
            // given
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL);
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), any(), anyInt(), any()))
                    .willThrow(new RuntimeException("AI 호출 실패"));

            // when & then
            assertThatThrownBy(() ->
                    questionGenerationService.generateQuestions(
                            1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                            types, List.of(), null, 30, TechStack.JAVA_SPRING))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("질문 생성 병렬 처리 실패");
        }

        @Test
        @DisplayName("FreshProvider 예외 발생 시 RuntimeException이 전파된다")
        void freshProviderThrows_propagatesRuntimeException() {
            // given
            List<InterviewType> types = List.of(InterviewType.RESUME_BASED);
            given(freshProvider.provide(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                    .willThrow(new RuntimeException("AI 호출 실패"));

            // when & then
            assertThatThrownBy(() ->
                    questionGenerationService.generateQuestions(
                            1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                            types, List.of(), "이력서", 30, TechStack.JAVA_SPRING))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("질문 생성 병렬 처리 실패");
        }
    }

    @Nested
    @DisplayName("generateQuestions - orderIndex 재배정")
    class OrderIndexReassignment {

        @Test
        @DisplayName("여러 QuestionSet의 orderIndex가 0부터 순차적으로 재배정된다")
        void multipleQuestionSets_orderIndexReassigned() {
            // given
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL);
            QuestionPool pool1 = makePool("CS 질문 1", "MODEL_ANSWER");
            QuestionPool pool2 = makePool("CS 질문 2", "GUIDE");
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(pool1, pool2));

            // when
            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), null, 30, TechStack.JAVA_SPRING);

            // then: saveResults에 전달된 QuestionSet 목록의 orderIndex를 캡처하여 검증
            org.mockito.ArgumentCaptor<List<QuestionSet>> captor =
                    org.mockito.ArgumentCaptor.forClass(List.class);
            then(transactionHandler).should().saveResults(eq(1L), captor.capture());

            List<QuestionSet> saved = captor.getValue();
            assertThat(saved).hasSize(2);
            assertThat(saved.get(0).getOrderIndex()).isEqualTo(0);
            assertThat(saved.get(1).getOrderIndex()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("generateQuestions - techStack 처리")
    class TechStackHandling {

        @Test
        @DisplayName("techStack이 null이면 Position 기반 기본값이 사용된다")
        void nullTechStack_usesPositionDefault() {
            // given
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL);
            QuestionPool pool = makePool("CS 질문", "MODEL_ANSWER");
            // TechStack.getDefaultForPosition(BACKEND) = JAVA_SPRING
            given(cacheableProvider.provide(anyLong(), any(), any(), eq(TechStack.JAVA_SPRING),
                    eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(pool));

            // when
            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), null, 30, null);

            // then
            then(cacheableProvider).should().provide(anyLong(), any(), any(),
                    eq(TechStack.JAVA_SPRING), eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any());
        }

        @Test
        @DisplayName("techStack이 명시되면 해당 techStack이 그대로 사용된다")
        void explicitTechStack_usedAsIs() {
            // given
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL);
            QuestionPool pool = makePool("CS 질문", "MODEL_ANSWER");
            given(cacheableProvider.provide(anyLong(), any(), any(), eq(TechStack.NODE_NESTJS),
                    eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(pool));

            // when
            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), null, 30, TechStack.NODE_NESTJS);

            // then
            then(cacheableProvider).should().provide(anyLong(), any(), any(),
                    eq(TechStack.NODE_NESTJS), eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any());
        }
    }

    @Nested
    @DisplayName("generateQuestions - QuestionSet 구조 및 FeedbackPerspective")
    class QuestionSetStructure {

        @Test
        @DisplayName("CacheableProvider 결과에 Question이 포함된 QuestionSet이 생성된다")
        void cacheableResult_questionSetContainsQuestion() {
            // given
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL);
            QuestionPool pool = makePool("Spring IoC 설명", "MODEL_ANSWER");
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(pool));

            // when
            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), null, 30, TechStack.JAVA_SPRING);

            // then
            org.mockito.ArgumentCaptor<List<QuestionSet>> captor =
                    org.mockito.ArgumentCaptor.forClass(List.class);
            then(transactionHandler).should().saveResults(eq(1L), captor.capture());

            List<QuestionSet> saved = captor.getValue();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getQuestions()).hasSize(1);
        }

        @Test
        @DisplayName("BEHAVIORAL 타입은 FeedbackPerspective.BEHAVIORAL로 설정된다")
        void behavioralType_feedbackPerspectiveBehavioral() {
            // given
            List<InterviewType> types = List.of(InterviewType.BEHAVIORAL);
            QuestionPool pool = makePool("자기소개를 해보세요", "GUIDE");
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), eq(InterviewType.BEHAVIORAL), anyInt(), any()))
                    .willReturn(List.of(pool));

            // when
            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), null, 30, TechStack.JAVA_SPRING);

            // then
            org.mockito.ArgumentCaptor<List<QuestionSet>> captor =
                    org.mockito.ArgumentCaptor.forClass(List.class);
            then(transactionHandler).should().saveResults(eq(1L), captor.capture());

            Question question = captor.getValue().get(0).getQuestions().get(0);
            assertThat(question.getFeedbackPerspective()).isEqualTo(FeedbackPerspective.BEHAVIORAL);
        }

        @Test
        @DisplayName("RESUME_BASED 타입(fresh)은 FeedbackPerspective.EXPERIENCE로 설정된다")
        void resumeBasedType_feedbackPerspectiveExperience() {
            // given
            List<InterviewType> types = List.of(InterviewType.RESUME_BASED);
            GeneratedQuestion gq = makeGeneratedQuestion("프로젝트 경험을 말해보세요", "RESUME", "GUIDE");
            given(freshProvider.provide(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                    .willReturn(List.of(gq));

            // when
            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), "이력서 내용", 30, TechStack.JAVA_SPRING);

            // then
            org.mockito.ArgumentCaptor<List<QuestionSet>> captor =
                    org.mockito.ArgumentCaptor.forClass(List.class);
            then(transactionHandler).should().saveResults(eq(1L), captor.capture());

            Question question = captor.getValue().get(0).getQuestions().get(0);
            assertThat(question.getFeedbackPerspective()).isEqualTo(FeedbackPerspective.EXPERIENCE);
        }

        @Test
        @DisplayName("CS_FUNDAMENTAL 타입은 FeedbackPerspective.TECHNICAL로 설정된다")
        void csFundamentalType_feedbackPerspectiveTechnical() {
            // given
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL);
            QuestionPool pool = makePool("OS 스케줄링 설명", "MODEL_ANSWER");
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(pool));

            // when
            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), null, 30, TechStack.JAVA_SPRING);

            // then
            org.mockito.ArgumentCaptor<List<QuestionSet>> captor =
                    org.mockito.ArgumentCaptor.forClass(List.class);
            then(transactionHandler).should().saveResults(eq(1L), captor.capture());

            Question question = captor.getValue().get(0).getQuestions().get(0);
            assertThat(question.getFeedbackPerspective()).isEqualTo(FeedbackPerspective.TECHNICAL);
        }

        @Test
        @DisplayName("referenceType 문자열이 ReferenceType enum으로 파싱된다")
        void referenceType_parsedCorrectly() {
            // given
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL);
            QuestionPool pool = makePool("CS 질문", "MODEL_ANSWER");
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(pool));

            // when
            questionGenerationService.generateQuestions(
                    1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), null, 30, TechStack.JAVA_SPRING);

            // then
            org.mockito.ArgumentCaptor<List<QuestionSet>> captor =
                    org.mockito.ArgumentCaptor.forClass(List.class);
            then(transactionHandler).should().saveResults(eq(1L), captor.capture());

            Question question = captor.getValue().get(0).getQuestions().get(0);
            assertThat(question.getReferenceType()).isEqualTo(com.rehearse.api.domain.question.entity.ReferenceType.MODEL_ANSWER);
        }
    }
}
