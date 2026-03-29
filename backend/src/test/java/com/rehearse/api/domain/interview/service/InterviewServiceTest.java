package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.*;
import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.questionset.entity.QuestionCategory;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.Question;
import com.rehearse.api.domain.questionset.entity.QuestionType;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @InjectMocks
    private InterviewService interviewService;

    @Mock
    private InterviewFinder interviewFinder;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Mock
    private com.rehearse.api.domain.questionset.service.QuestionSetService questionSetService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("READY -> IN_PROGRESS 상태 전이 성공 (질문 생성 완료 시)")
    void updateStatus_readyToInProgress() {
        Interview interview = createMockInterview();
        interview.completeQuestionGeneration();
        given(interviewFinder.findById(1L)).willReturn(interview);

        UpdateStatusRequest request = new UpdateStatusRequest();
        ReflectionTestUtils.setField(request, "status", InterviewStatus.IN_PROGRESS);

        UpdateStatusResponse response = interviewService.updateStatus(1L, request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("질문 생성 미완료 시 IN_PROGRESS 전환 실패")
    void updateStatus_questionGenerationNotCompleted() {
        Interview interview = createMockInterview(); // PENDING 상태
        given(interviewFinder.findById(1L)).willReturn(interview);

        UpdateStatusRequest request = new UpdateStatusRequest();
        ReflectionTestUtils.setField(request, "status", InterviewStatus.IN_PROGRESS);

        assertThatThrownBy(() -> interviewService.updateStatus(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("INTERVIEW_004");
                });
    }

    @Test
    @DisplayName("존재하지 않는 면접 세션 상태 변경 시 BusinessException이 발생한다")
    void updateStatus_notFound() {
        given(interviewFinder.findById(999L))
                .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "INTERVIEW_001", "면접 세션을 찾을 수 없습니다."));

        UpdateStatusRequest request = new UpdateStatusRequest();
        ReflectionTestUtils.setField(request, "status", InterviewStatus.IN_PROGRESS);

        assertThatThrownBy(() -> interviewService.updateStatus(999L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("INTERVIEW_001");
                });
    }

    @Test
    @DisplayName("READY -> COMPLETED 잘못된 상태 전이 시 BusinessException이 발생한다")
    void updateStatus_invalidTransition() {
        Interview interview = createMockInterview();
        interview.completeQuestionGeneration();
        given(interviewFinder.findById(1L)).willReturn(interview);

        UpdateStatusRequest request = new UpdateStatusRequest();
        ReflectionTestUtils.setField(request, "status", InterviewStatus.COMPLETED);

        assertThatThrownBy(() -> interviewService.updateStatus(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("INTERVIEW_002");
                });
    }

    @Test
    @DisplayName("FAILED 상태에서 질문 생성 재시도 성공")
    void retryQuestionGeneration_success() {
        Interview interview = createMockInterview();
        interview.failQuestionGeneration("Claude API timeout");
        given(interviewFinder.findById(1L)).willReturn(interview);
        given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(Collections.emptyList());

        InterviewResponse response = interviewService.retryQuestionGeneration(1L);

        assertThat(response.getQuestionGenerationStatus()).isEqualTo(QuestionGenerationStatus.PENDING);
        assertThat(response.getFailureReason()).isNull();
        then(eventPublisher).should().publishEvent(any(QuestionGenerationRequestedEvent.class));
    }

    @Test
    @DisplayName("FAILED가 아닌 상태에서 재시도 시 예외 발생")
    void retryQuestionGeneration_notFailed() {
        Interview interview = createMockInterview(); // PENDING
        given(interviewFinder.findById(1L)).willReturn(interview);

        assertThatThrownBy(() -> interviewService.retryQuestionGeneration(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("INTERVIEW_005");
                });
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

    private QuestionSet createMockQuestionSet(Interview interview) {
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
}
