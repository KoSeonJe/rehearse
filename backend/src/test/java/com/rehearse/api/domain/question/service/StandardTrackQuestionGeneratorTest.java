package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
@DisplayName("StandardTrackQuestionGenerator")
class StandardTrackQuestionGeneratorTest {

    @Mock
    private CacheableQuestionProvider cacheableProvider;

    @Mock
    private FreshQuestionProvider freshProvider;

    private StandardTrackQuestionGenerator generator;

    private final QuestionSetAssembler assembler = new QuestionSetAssembler();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        generator = new StandardTrackQuestionGenerator(cacheableProvider, freshProvider, assembler);
    }

    private QuestionPool makePool(String content, String referenceType) {
        return QuestionPool.create("key:cs:junior", content, null, null, null, referenceType);
    }

    private GeneratedQuestion makeGenerated(String content, String questionCategory, String referenceType) {
        GeneratedQuestion gq = new GeneratedQuestion();
        ReflectionTestUtils.setField(gq, "content", content);
        ReflectionTestUtils.setField(gq, "ttsContent", null);
        ReflectionTestUtils.setField(gq, "questionCategory", questionCategory);
        ReflectionTestUtils.setField(gq, "modelAnswer", null);
        ReflectionTestUtils.setField(gq, "referenceType", referenceType);
        return gq;
    }

    @Nested
    @DisplayName("Provider 호출 분기")
    class ProviderCallBranch {

        @Test
        @DisplayName("cacheable 타입만 여럿 있을 때 CacheableProvider 가 타입별로 호출된다")
        void multiCacheableTypes_callsCacheableProviderForEachType() {
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL, InterviewType.BEHAVIORAL);
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(makePool("CS 질문", "MODEL_ANSWER")));
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), eq(InterviewType.BEHAVIORAL), anyInt(), any()))
                    .willReturn(List.of(makePool("자기소개", "GUIDE")));

            generator.generate(1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    types, List.of(), null, 30, TechStack.JAVA_SPRING);

            then(cacheableProvider).should(org.mockito.Mockito.times(2))
                    .provide(anyLong(), any(), any(), any(), any(), anyInt(), any());
            then(freshProvider).should(never()).provide(any(), any(), any(), any(), anyInt(), any(), any(), any());
        }

        @Test
        @DisplayName("cacheable 타입만 있을 때 FreshProvider 는 호출되지 않는다")
        void cacheableOnly_doesNotCallFreshProvider() {
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(makePool("CS 질문", "MODEL_ANSWER")));

            generator.generate(1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    List.of(InterviewType.CS_FUNDAMENTAL), List.of(), null, 30, TechStack.JAVA_SPRING);

            then(freshProvider).should(never()).provide(any(), any(), any(), any(), anyInt(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("예외 처리")
    class ExceptionHandling {

        @Test
        @DisplayName("CacheableProvider 예외 시 RuntimeException이 전파된다")
        void cacheableProviderThrows_propagatesRuntimeException() {
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), any(), anyInt(), any()))
                    .willThrow(new RuntimeException("AI 호출 실패"));

            assertThatThrownBy(() -> generator.generate(1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    List.of(InterviewType.CS_FUNDAMENTAL), List.of(), null, 30, TechStack.JAVA_SPRING))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("질문 생성 병렬 처리 실패");
        }
    }

    @Nested
    @DisplayName("orderIndex 재배정")
    class OrderIndex {

        @Test
        @DisplayName("여러 QuestionSet 의 orderIndex 가 0부터 순차적으로 재배정된다")
        void multipleQuestionSets_orderIndexReassigned() {
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(makePool("Q1", "MODEL_ANSWER"), makePool("Q2", "GUIDE")));

            List<QuestionSet> result = generator.generate(1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    List.of(InterviewType.CS_FUNDAMENTAL), List.of(), null, 30, TechStack.JAVA_SPRING);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getOrderIndex()).isEqualTo(0);
            assertThat(result.get(1).getOrderIndex()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("techStack 처리")
    class TechStackHandling {

        @Test
        @DisplayName("techStack 이 null 이면 Position 기반 기본값이 사용된다")
        void nullTechStack_usesPositionDefault() {
            given(cacheableProvider.provide(anyLong(), any(), any(), eq(TechStack.JAVA_SPRING),
                    eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(makePool("CS 질문", "MODEL_ANSWER")));

            generator.generate(1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    List.of(InterviewType.CS_FUNDAMENTAL), List.of(), null, 30, null);

            then(cacheableProvider).should().provide(anyLong(), any(), any(),
                    eq(TechStack.JAVA_SPRING), eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any());
        }
    }

    @Nested
    @DisplayName("QuestionSet 구조 및 FeedbackPerspective")
    class QuestionSetStructure {

        @Test
        @DisplayName("BEHAVIORAL 타입은 FeedbackPerspective.BEHAVIORAL 로 설정된다")
        void behavioralType_perspectiveBehavioral() {
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), eq(InterviewType.BEHAVIORAL), anyInt(), any()))
                    .willReturn(List.of(makePool("자기소개", "GUIDE")));

            List<QuestionSet> result = generator.generate(1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    List.of(InterviewType.BEHAVIORAL), List.of(), null, 30, TechStack.JAVA_SPRING);

            Question q = result.get(0).getQuestions().get(0);
            assertThat(q.getFeedbackPerspective()).isEqualTo(FeedbackPerspective.BEHAVIORAL);
        }

        @Test
        @DisplayName("CS_FUNDAMENTAL 타입은 FeedbackPerspective.TECHNICAL 로 설정된다")
        void csFundamental_perspectiveTechnical() {
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(makePool("OS 스케줄링", "MODEL_ANSWER")));

            List<QuestionSet> result = generator.generate(1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    List.of(InterviewType.CS_FUNDAMENTAL), List.of(), null, 30, TechStack.JAVA_SPRING);

            Question q = result.get(0).getQuestions().get(0);
            assertThat(q.getFeedbackPerspective()).isEqualTo(FeedbackPerspective.TECHNICAL);
        }

        @Test
        @DisplayName("referenceType 문자열이 ReferenceType enum 으로 파싱된다")
        void referenceType_parsedCorrectly() {
            given(cacheableProvider.provide(anyLong(), any(), any(), any(), eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(makePool("CS 질문", "MODEL_ANSWER")));

            List<QuestionSet> result = generator.generate(1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                    List.of(InterviewType.CS_FUNDAMENTAL), List.of(), null, 30, TechStack.JAVA_SPRING);

            Question q = result.get(0).getQuestions().get(0);
            assertThat(q.getReferenceType()).isEqualTo(ReferenceType.MODEL_ANSWER);
        }
    }
}
