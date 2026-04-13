package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.global.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class QuestionGenerationTransactionHandlerTest {

    @InjectMocks
    private QuestionGenerationTransactionHandler handler;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Nested
    @DisplayName("startGeneration 메서드")
    class StartGeneration {

        @Test
        @DisplayName("startGeneration - 정상: 면접 ID로 질문 생성 시작 상태로 전환한다")
        void startGeneration_success() {
            // given
            Interview interview = TestFixtures.createInterview();
            ReflectionTestUtils.setField(interview, "id", 1L);
            given(interviewRepository.findById(1L)).willReturn(Optional.of(interview));

            // when
            handler.startGeneration(1L);

            // then
            then(interviewRepository).should().findById(1L);
        }

        @Test
        @DisplayName("startGeneration - 예외: 면접이 존재하지 않으면 BusinessException이 발생한다")
        void startGeneration_interviewNotFound_throwsBusinessException() {
            // given
            given(interviewRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> handler.startGeneration(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_001");
                    });
        }
    }

    @Nested
    @DisplayName("saveResults 메서드")
    class SaveResults {

        @Test
        @DisplayName("saveResults - 정상: QuestionSet 목록을 면접에 할당하고 저장한다")
        void saveResults_success() {
            // given
            Interview interview = TestFixtures.createInterview();
            ReflectionTestUtils.setField(interview, "id", 1L);
            interview.startQuestionGeneration();
            given(interviewRepository.findById(1L)).willReturn(Optional.of(interview));

            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);
            given(questionSetRepository.saveAll(any())).willReturn(List.of(questionSet));

            // when
            handler.saveResults(1L, List.of(questionSet));

            // then
            then(questionSetRepository).should().saveAll(any());
        }

        @Test
        @DisplayName("saveResults - 정상: 여러 QuestionSet을 한 번에 저장한다")
        void saveResults_multipleQuestionSets() {
            // given
            Interview interview = TestFixtures.createInterview();
            ReflectionTestUtils.setField(interview, "id", 1L);
            interview.startQuestionGeneration();
            given(interviewRepository.findById(1L)).willReturn(Optional.of(interview));

            QuestionSet qs1 = TestFixtures.createQuestionSet(interview, QuestionSetCategory.CS_FUNDAMENTAL, 0);
            QuestionSet qs2 = TestFixtures.createQuestionSet(interview, QuestionSetCategory.BEHAVIORAL, 1);
            List<QuestionSet> questionSets = List.of(qs1, qs2);
            given(questionSetRepository.saveAll(any())).willReturn(questionSets);

            // when
            handler.saveResults(1L, questionSets);

            // then
            then(questionSetRepository).should().saveAll(questionSets);
        }

        @Test
        @DisplayName("saveResults - 예외: 면접이 존재하지 않으면 BusinessException이 발생한다")
        void saveResults_interviewNotFound_throwsBusinessException() {
            // given
            given(interviewRepository.findById(999L)).willReturn(Optional.empty());

            QuestionSet questionSet = TestFixtures.createQuestionSet(TestFixtures.createInterview());

            // when & then
            assertThatThrownBy(() -> handler.saveResults(999L, List.of(questionSet)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_001");
                    });
        }
    }

    @Nested
    @DisplayName("failGeneration 메서드")
    class FailGeneration {

        @Test
        @DisplayName("failGeneration - 정상: 면접 존재 시 실패 상태로 전환한다")
        void failGeneration_success() {
            // given
            Interview interview = TestFixtures.createInterview();
            ReflectionTestUtils.setField(interview, "id", 1L);
            interview.startQuestionGeneration();
            given(interviewRepository.findById(1L)).willReturn(Optional.of(interview));

            // when
            handler.failGeneration(1L, "AI 서비스 오류");

            // then
            then(interviewRepository).should().findById(1L);
        }

        @Test
        @DisplayName("failGeneration - 면접이 존재하지 않으면 아무 작업도 수행하지 않는다 (ifPresent 처리)")
        void failGeneration_interviewNotFound_doesNothing() {
            // given
            given(interviewRepository.findById(999L)).willReturn(Optional.empty());

            // when
            handler.failGeneration(999L, "AI 서비스 오류");

            // then
            then(questionSetRepository).shouldHaveNoInteractions();
        }
    }
}
