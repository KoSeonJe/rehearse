package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.feedback.rubric.entity.RubricCategory;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.question.dto.QuestionGenerationCommand;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("StandardTrackQuestionGenerator")
class StandardTrackQuestionGeneratorTest {

    @Mock
    private StandardQuestionProvider standardProvider;

    private StandardTrackQuestionGenerator generator;

    private final QuestionSetAssembler assembler = new QuestionSetAssembler();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        generator = new StandardTrackQuestionGenerator(standardProvider, assembler);
    }

    private QuestionPool makePool(String content) {
        return QuestionPool.create("key:cs:junior", content, null, null, null);
    }

    private QuestionGenerationCommand command(List<InterviewType> types, TechStack techStack) {
        return new QuestionGenerationCommand(
                1L, 1L, Position.BACKEND, InterviewLevel.JUNIOR,
                types, List.of(), null, 30, techStack);
    }

    @Nested
    @DisplayName("Provider 호출 분기")
    class ProviderCallBranch {

        @Test
        @DisplayName("여러 타입이 있을 때 CacheableProvider 가 타입별로 호출된다")
        void multipleTypes_callsCacheableProviderForEachType() {
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL, InterviewType.BEHAVIORAL);
            given(standardProvider.provide(eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(makePool("CS 질문")));
            given(standardProvider.provide(eq(InterviewType.BEHAVIORAL), anyInt(), any()))
                    .willReturn(List.of(makePool("자기소개")));

            generator.generate(command(types, TechStack.JAVA_SPRING));

            then(standardProvider).should(org.mockito.Mockito.times(2))
                    .provide(any(), anyInt(), any());
        }
    }

    @Nested
    @DisplayName("예외 처리")
    class ExceptionHandling {

        @Test
        @DisplayName("CacheableProvider 예외 시 그대로 전파된다")
        void standardProviderThrows_propagatesException() {
            given(standardProvider.provide(any(), anyInt(), any()))
                    .willThrow(new RuntimeException("AI 호출 실패"));

            assertThatThrownBy(() -> generator.generate(
                    command(List.of(InterviewType.CS_FUNDAMENTAL), TechStack.JAVA_SPRING)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("AI 호출 실패");
        }
    }

    @Nested
    @DisplayName("orderIndex 재배정")
    class OrderIndex {

        @Test
        @DisplayName("여러 QuestionSet 의 orderIndex 가 0부터 순차적으로 재배정된다")
        void multipleQuestionSets_orderIndexReassigned() {
            given(standardProvider.provide(eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(makePool("Q1"), makePool("Q2")));

            List<QuestionSet> result = generator.generate(
                    command(List.of(InterviewType.CS_FUNDAMENTAL), TechStack.JAVA_SPRING));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getOrderIndex()).isEqualTo(0);
            assertThat(result.get(1).getOrderIndex()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("techStack 전달")
    class TechStackPassthrough {

        @Test
        @DisplayName("Command 의 techStack 이 그대로 CacheableProvider 로 전달된다")
        void techStackPassedThrough() {
            given(standardProvider.provide(eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(makePool("CS 질문")));

            generator.generate(command(List.of(InterviewType.CS_FUNDAMENTAL), TechStack.JAVA_SPRING));

            then(standardProvider).should().provide(eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any());
        }
    }

    @Nested
    @DisplayName("QuestionSet 구조 - sub-type 결정")
    class QuestionSetStructure {

        @Test
        @DisplayName("BEHAVIORAL 타입은 BEHAVIORAL_MAIN 으로 적재되고 enum 환원 시 (GUIDE, BEHAVIORAL)")
        void behavioralType_assignsBehavioralMain() {
            given(standardProvider.provide(eq(InterviewType.BEHAVIORAL), anyInt(), any()))
                    .willReturn(List.of(makePool("자기소개")));

            List<QuestionSet> result = generator.generate(
                    command(List.of(InterviewType.BEHAVIORAL), TechStack.JAVA_SPRING));

            Question q = result.get(0).getQuestions().get(0);
            assertThat(q.getQuestionType()).isEqualTo(QuestionType.BEHAVIORAL_MAIN);
            assertThat(q.getQuestionType().referenceType()).isEqualTo(ReferenceType.GUIDE);
            assertThat(q.getQuestionType().rubricCategory()).isEqualTo(RubricCategory.BEHAVIORAL);
        }

        @Test
        @DisplayName("CS_FUNDAMENTAL 타입은 TECH_MAIN 으로 적재되고 enum 환원 시 (MODEL_ANSWER, TECHNICAL)")
        void csFundamental_assignsTechMain() {
            given(standardProvider.provide(eq(InterviewType.CS_FUNDAMENTAL), anyInt(), any()))
                    .willReturn(List.of(makePool("OS 스케줄링")));

            List<QuestionSet> result = generator.generate(
                    command(List.of(InterviewType.CS_FUNDAMENTAL), TechStack.JAVA_SPRING));

            Question q = result.get(0).getQuestions().get(0);
            assertThat(q.getQuestionType()).isEqualTo(QuestionType.TECH_MAIN);
            assertThat(q.getQuestionType().referenceType()).isEqualTo(ReferenceType.MODEL_ANSWER);
            assertThat(q.getQuestionType().rubricCategory()).isEqualTo(RubricCategory.TECHNICAL);
        }
    }
}
