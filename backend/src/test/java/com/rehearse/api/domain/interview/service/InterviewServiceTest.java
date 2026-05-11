package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.*;
import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.domain.resume.entity.ResumeSkeletonEntity;
import com.rehearse.api.domain.resume.repository.ResumeSkeletonRepository;
import com.rehearse.api.global.config.InterviewProperties;
import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewService - 면접 세션 관리")
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
    private com.rehearse.api.domain.question.service.QuestionSetService questionSetService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ResumeSkeletonRepository resumeSkeletonRepository;

    @Mock
    private InterviewRetryRecorder interviewRetryRecorder;

    @org.mockito.Spy
    private InterviewProperties interviewProperties = new InterviewProperties(
            new InterviewProperties.Retry(5, 30),
            new InterviewProperties.Audio(
                    10L * 1024 * 1024,
                    300,
                    java.util.Set.of("audio/webm", "audio/mp4", "audio/mpeg", "audio/wav")
            )
    );

    @Nested
    @DisplayName("getInterview 메서드")
    class GetInterview {

        @Test
        @DisplayName("존재하지 않는 면접 세션 조회 시 BusinessException이 발생한다")
        void getInterview_notFound() {
            given(interviewFinder.findById(999L))
                    .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "INTERVIEW_001", "면접 세션을 찾을 수 없습니다."));

            assertThatThrownBy(() -> interviewService.getInterview(999L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_001");
                    });
        }

        @Test
        @DisplayName("면접 세션 조회 성공")
        void getInterview_success() {
            Interview interview = createMockInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);
            given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(List.of());

            InterviewResponse response = interviewService.getInterview(1L, 1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getPosition()).isEqualTo(Position.BACKEND);
            assertThat(response.getStatus()).isEqualTo(InterviewStatus.READY);
            assertThat(response.getQuestionGenerationStatus()).isEqualTo(QuestionGenerationStatus.PENDING);
        }

        @Test
        @DisplayName("소유자가 다른 면접 세션 조회 시 INTERVIEW_NOT_FOUND BusinessException이 발생한다")
        void getInterview_differentOwner_notFound() {
            Interview interview = createMockInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);

            assertThatThrownBy(() -> interviewService.getInterview(1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("INTERVIEW_001"));
        }
    }

    @Nested
    @DisplayName("getInterviewByPublicId 메서드")
    class GetInterviewByPublicId {

        @Test
        @DisplayName("본인 publicId 조회 성공")
        void getInterviewByPublicId_owner_success() {
            Interview interview = createMockInterview();
            String publicId = "test-public-uuid";
            ReflectionTestUtils.setField(interview, "publicId", publicId);
            given(interviewFinder.findByPublicId(publicId)).willReturn(interview);
            given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(List.of());

            InterviewResponse response = interviewService.getInterviewByPublicId(publicId, 1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getInterviewTypes()).containsExactly(InterviewType.CS_FUNDAMENTAL);
        }

        @Test
        @DisplayName("타 유저 publicId 조회 시 INTERVIEW_NOT_FOUND 예외")
        void getInterviewByPublicId_otherUser_notFound() {
            String publicId = "test-public-uuid";
            Interview interview = createMockInterview();
            ReflectionTestUtils.setField(interview, "publicId", publicId);
            given(interviewFinder.findByPublicId(publicId)).willReturn(interview);

            assertThatThrownBy(() -> interviewService.getInterviewByPublicId(publicId, 2L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("INTERVIEW_001"));
        }
    }

    @Nested
    @DisplayName("updateStatus 메서드")
    class UpdateStatus {

        @Test
        @DisplayName("READY -> IN_PROGRESS 상태 전이 성공 (질문 생성 완료 시)")
        void updateStatus_readyToInProgress() {
            // given
            Interview interview = createMockInterview();
            interview.completeQuestionGeneration();
            given(interviewFinder.findById(1L)).willReturn(interview);

            UpdateStatusRequest request = new UpdateStatusRequest();
            ReflectionTestUtils.setField(request, "status", InterviewStatus.IN_PROGRESS);

            // when
            UpdateStatusResponse response = interviewService.updateStatus(1L, 1L, request);

            // then
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("질문 생성 미완료 시 IN_PROGRESS 전환 실패")
        void updateStatus_questionGenerationNotCompleted() {
            // given
            Interview interview = createMockInterview(); // PENDING 상태
            given(interviewFinder.findById(1L)).willReturn(interview);

            UpdateStatusRequest request = new UpdateStatusRequest();
            ReflectionTestUtils.setField(request, "status", InterviewStatus.IN_PROGRESS);

            // when & then
            assertThatThrownBy(() -> interviewService.updateStatus(1L, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_004");
                    });
        }

        @Test
        @DisplayName("존재하지 않는 면접 세션 상태 변경 시 BusinessException이 발생한다")
        void updateStatus_notFound() {
            // given
            given(interviewFinder.findById(999L))
                    .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "INTERVIEW_001", "면접 세션을 찾을 수 없습니다."));

            UpdateStatusRequest request = new UpdateStatusRequest();
            ReflectionTestUtils.setField(request, "status", InterviewStatus.IN_PROGRESS);

            // when & then
            assertThatThrownBy(() -> interviewService.updateStatus(999L, 1L, request))
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
            // given
            Interview interview = createMockInterview();
            interview.completeQuestionGeneration();
            given(interviewFinder.findById(1L)).willReturn(interview);

            UpdateStatusRequest request = new UpdateStatusRequest();
            ReflectionTestUtils.setField(request, "status", InterviewStatus.COMPLETED);

            // when & then
            assertThatThrownBy(() -> interviewService.updateStatus(1L, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_002");
                    });
        }
    }

    @Nested
    @DisplayName("retryQuestionGeneration 메서드")
    class RetryQuestionGeneration {

        @Test
        @DisplayName("FAILED 상태에서 질문 생성 재시도 성공")
        void retryQuestionGeneration_success() {
            // given
            Interview interview = createMockInterview();
            interview.failQuestionGeneration("Claude API timeout");
            given(interviewFinder.findById(1L)).willReturn(interview);
            given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(Collections.emptyList());

            // when
            InterviewResponse response = interviewService.retryQuestionGeneration(1L, 1L);

            // then
            assertThat(response.getQuestionGenerationStatus()).isEqualTo(QuestionGenerationStatus.PENDING);
            assertThat(response.getFailureReason()).isNull();
            then(eventPublisher).should().publishEvent(any(QuestionGenerationRequestedEvent.class));
        }

        @Test
        @DisplayName("FAILED가 아닌 상태에서 재시도 시 예외 발생")
        void retryQuestionGeneration_notFailed() {
            // given
            Interview interview = createMockInterview(); // PENDING
            given(interviewFinder.findById(1L)).willReturn(interview);

            // when & then
            assertThatThrownBy(() -> interviewService.retryQuestionGeneration(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_005");
                    });
        }

        @Test
        @DisplayName("재시도 한도(5회)를 초과하면 RETRY_LIMIT_EXCEEDED")
        void retryQuestionGeneration_limitExceeded() {
            // given
            Interview interview = createMockInterview();
            interview.failQuestionGeneration("AI 호출 실패");
            ReflectionTestUtils.setField(interview, "questionGenRetryCount", 5);
            given(interviewFinder.findById(1L)).willReturn(interview);

            // when & then
            assertThatThrownBy(() -> interviewService.retryQuestionGeneration(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_012");
                    });
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("쿨다운(30초) 미경과 시 RETRY_COOLDOWN")
        void retryQuestionGeneration_cooldownNotExpired() {
            // given
            Interview interview = createMockInterview();
            interview.failQuestionGeneration("AI 호출 실패");
            ReflectionTestUtils.setField(interview, "questionGenRetryCount", 1);
            ReflectionTestUtils.setField(interview, "questionGenLastRetriedAt",
                    LocalDateTime.now().minusSeconds(5));
            given(interviewFinder.findById(1L)).willReturn(interview);

            // when & then
            assertThatThrownBy(() -> interviewService.retryQuestionGeneration(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_013");
                    });
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("RESUME_BASED 인데 skeleton 부재 시 RESUME_PLAN_RECOVERY_REQUIRED")
        void retryQuestionGeneration_resumeBasedSkeletonMissing() {
            // given
            Interview interview = Interview.builder()
                    .position(Position.BACKEND)
                    .level(InterviewLevel.JUNIOR)
                    .interviewTypes(List.of(InterviewType.RESUME_BASED))
                    .durationMinutes(30)
                    .build();
            ReflectionTestUtils.setField(interview, "id", 1L);
            ReflectionTestUtils.setField(interview, "userId", 1L);
            interview.failQuestionGeneration("AI 호출 실패");
            given(interviewFinder.findById(1L)).willReturn(interview);
            given(resumeSkeletonRepository.findByInterviewId(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> interviewService.retryQuestionGeneration(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_014");
                    });
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("재시도 성공 시 retry counter 가 증가하고 이벤트가 발행된다")
        void retryQuestionGeneration_successIncrementsCounter() {
            // given
            Interview interview = createMockInterview();
            interview.failQuestionGeneration("AI timeout");
            given(interviewFinder.findById(1L)).willReturn(interview);
            given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(Collections.emptyList());

            // when
            interviewService.retryQuestionGeneration(1L, 1L);

            // then
            then(interviewRetryRecorder).should().record(interview);
            then(eventPublisher).should().publishEvent(any(QuestionGenerationRequestedEvent.class));
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

    private QuestionSet createMockQuestionSet(Interview interview) {
        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category(InterviewType.CS_FUNDAMENTAL)
                .orderIndex(0)
                .build();
        ReflectionTestUtils.setField(qs, "id", 10L);

        Question mainQuestion = Question.builder()
                .questionType(QuestionType.TECH_MAIN)
                .questionText("HashMap과 TreeMap의 차이점은?")
                .orderIndex(0)
                .build();
        qs.addQuestion(mainQuestion);
        return qs;
    }
}
