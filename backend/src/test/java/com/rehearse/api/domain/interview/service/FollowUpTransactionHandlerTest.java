package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.domain.questionset.entity.*;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.repository.QuestionRepository;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class FollowUpTransactionHandlerTest {

    @InjectMocks
    private FollowUpTransactionHandler handler;

    @Mock
    private InterviewFinder interviewFinder;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Test
    @DisplayName("loadFollowUpContext - 정상: IN_PROGRESS 면접에서 컨텍스트 로드")
    void loadFollowUpContext_success() {
        // given
        Interview interview = createInProgressInterview();
        given(interviewFinder.findById(1L)).willReturn(interview);

        QuestionSet questionSet = createQuestionSetWithMainQuestion(interview);
        given(questionSetRepository.findById(10L)).willReturn(Optional.of(questionSet));

        // when
        FollowUpContext context = handler.loadFollowUpContext(1L, 10L);

        // then
        assertThat(context.position()).isEqualTo(Position.BACKEND);
        assertThat(context.level()).isEqualTo(InterviewLevel.JUNIOR);
        assertThat(context.questionSetId()).isEqualTo(10L);
        assertThat(context.nextOrderIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("loadFollowUpContext - 예외: 면접이 IN_PROGRESS가 아닌 경우")
    void loadFollowUpContext_notInProgress() {
        // given
        Interview interview = createMockInterview(); // READY 상태
        given(interviewFinder.findById(1L)).willReturn(interview);

        // when & then
        assertThatThrownBy(() -> handler.loadFollowUpContext(1L, 10L))
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
        assertThatThrownBy(() -> handler.loadFollowUpContext(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("QUESTION_SET_001");
                });
    }

    @Test
    @DisplayName("loadFollowUpContext - 예외: 후속질문 라운드 초과")
    void loadFollowUpContext_maxFollowUpExceeded() {
        // given
        Interview interview = createInProgressInterview();
        given(interviewFinder.findById(1L)).willReturn(interview);

        QuestionSet questionSet = createQuestionSetWithFollowUps(interview, 2);
        given(questionSetRepository.findById(10L)).willReturn(Optional.of(questionSet));

        // when & then
        assertThatThrownBy(() -> handler.loadFollowUpContext(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("QUESTION_SET_004");
                });
    }

    @Test
    @DisplayName("saveFollowUpResult - 정상: 후속질문 저장")
    void saveFollowUpResult_success() {
        // given
        Interview interview = createInProgressInterview();
        QuestionSet questionSet = createQuestionSetWithMainQuestion(interview);
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
        given(questionRepository.save(any(Question.class))).willReturn(savedQuestion);

        // when
        Question result = handler.saveFollowUpResult(10L, followUp, 1);

        // then
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getQuestionText()).isEqualTo("해시 충돌 해결 방법은?");
        then(questionRepository).should().save(any(Question.class));
    }

    private Interview createMockInterview() {
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();
        ReflectionTestUtils.setField(interview, "id", 1L);
        return interview;
    }

    private Interview createInProgressInterview() {
        Interview interview = createMockInterview();
        interview.completeQuestionGeneration();
        interview.updateStatus(InterviewStatus.IN_PROGRESS);
        return interview;
    }

    private QuestionSet createQuestionSetWithMainQuestion(Interview interview) {
        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category(QuestionCategory.CS)
                .orderIndex(0)
                .build();
        ReflectionTestUtils.setField(qs, "id", 10L);

        Question mainQuestion = Question.builder()
                .questionType(QuestionType.MAIN)
                .questionText("HashMap과 TreeMap의 차이점은?")
                .orderIndex(0)
                .build();
        qs.addQuestion(mainQuestion);
        return qs;
    }

    private QuestionSet createQuestionSetWithFollowUps(Interview interview, int followUpCount) {
        QuestionSet qs = createQuestionSetWithMainQuestion(interview);
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
}
