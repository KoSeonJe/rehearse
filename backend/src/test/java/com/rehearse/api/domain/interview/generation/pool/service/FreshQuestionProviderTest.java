package com.rehearse.api.domain.interview.generation.pool.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class FreshQuestionProviderTest {

    @InjectMocks
    private FreshQuestionProvider freshQuestionProvider;

    @Mock
    private AiClient aiClient;

    private static final Position POSITION = Position.BACKEND;
    private static final InterviewLevel LEVEL = InterviewLevel.JUNIOR;
    private static final TechStack TECH_STACK = TechStack.JAVA_SPRING;
    private static final Set<InterviewType> TYPES = Set.of(InterviewType.LANGUAGE_FRAMEWORK);

    @Nested
    @DisplayName("생성 수량 처리")
    class QuantityHandling {

        @Test
        @DisplayName("AI가 requiredCount와 정확히 같은 수량을 생성하면 전체를 반환한다")
        void exactCount_returnsAll() {
            // given
            List<GeneratedQuestion> generated = makeQuestions(3);
            given(aiClient.generateQuestions(any(QuestionGenerationRequest.class))).willReturn(generated);

            // when
            List<GeneratedQuestion> result = freshQuestionProvider.provide(
                    POSITION, LEVEL, TECH_STACK, TYPES, 3, null, null, 30);

            // then
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("AI가 requiredCount보다 많이 생성하면 requiredCount만큼 잘라서 반환한다")
        void exceededCount_truncatesToRequired() {
            // given
            List<GeneratedQuestion> generated = makeQuestions(5);
            given(aiClient.generateQuestions(any(QuestionGenerationRequest.class))).willReturn(generated);

            // when
            List<GeneratedQuestion> result = freshQuestionProvider.provide(
                    POSITION, LEVEL, TECH_STACK, TYPES, 3, null, null, 30);

            // then
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("AI가 requiredCount보다 적게 생성하면 있는 만큼 반환한다")
        void insufficientCount_returnsAll() {
            // given
            List<GeneratedQuestion> generated = makeQuestions(2);
            given(aiClient.generateQuestions(any(QuestionGenerationRequest.class))).willReturn(generated);

            // when
            List<GeneratedQuestion> result = freshQuestionProvider.provide(
                    POSITION, LEVEL, TECH_STACK, TYPES, 5, null, null, 30);

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("요청 파라미터 전달")
    class RequestParameters {

        @Test
        @DisplayName("csSubTopics가 null이면 QuestionGenerationRequest에 빈 Set을 전달한다")
        void nullCsSubTopics_passesEmptySet() {
            // given
            given(aiClient.generateQuestions(any(QuestionGenerationRequest.class)))
                    .willReturn(makeQuestions(1));
            ArgumentCaptor<QuestionGenerationRequest> captor =
                    ArgumentCaptor.forClass(QuestionGenerationRequest.class);

            // when
            freshQuestionProvider.provide(POSITION, LEVEL, TECH_STACK, TYPES, 1, null, null, 30);

            // then
            then(aiClient).should().generateQuestions(captor.capture());
            assertThat(captor.getValue().csSubTopics()).isEmpty();
        }

        @Test
        @DisplayName("resumeText가 null이면 QuestionGenerationRequest에 null로 전달한다")
        void nullResumeText_passesNullToRequest() {
            // given
            given(aiClient.generateQuestions(any(QuestionGenerationRequest.class)))
                    .willReturn(makeQuestions(1));
            ArgumentCaptor<QuestionGenerationRequest> captor =
                    ArgumentCaptor.forClass(QuestionGenerationRequest.class);

            // when
            freshQuestionProvider.provide(POSITION, LEVEL, TECH_STACK, TYPES, 1, null, null, 30);

            // then
            then(aiClient).should().generateQuestions(captor.capture());
            assertThat(captor.getValue().resumeText()).isNull();
        }

        @Test
        @DisplayName("types가 빈 Set이면 QuestionGenerationRequest에 빈 Set을 전달한다")
        void emptyTypes_passesEmptySetToRequest() {
            // given
            given(aiClient.generateQuestions(any(QuestionGenerationRequest.class)))
                    .willReturn(Collections.emptyList());
            ArgumentCaptor<QuestionGenerationRequest> captor =
                    ArgumentCaptor.forClass(QuestionGenerationRequest.class);

            // when
            freshQuestionProvider.provide(POSITION, LEVEL, TECH_STACK, Set.of(), 1, null, null, 30);

            // then
            then(aiClient).should().generateQuestions(captor.capture());
            assertThat(captor.getValue().interviewTypes()).isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────

    private List<GeneratedQuestion> makeQuestions(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> {
                    GeneratedQuestion gq = new GeneratedQuestion();
                    ReflectionTestUtils.setField(gq, "content", "질문" + i);
                    return gq;
                })
                .toList();
    }
}
