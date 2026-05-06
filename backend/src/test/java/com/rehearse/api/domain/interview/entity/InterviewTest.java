package com.rehearse.api.domain.interview.entity;

import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Interview - 면접 엔티티")
class InterviewTest {

    @Nested
    @DisplayName("getEffectiveTechStack 메서드")
    class GetEffectiveTechStack {

        @Test
        @DisplayName("techStack이 null이고 BACKEND이면 JAVA_SPRING을 반환한다")
        void nullTechStack_backendPosition_returnsJavaSpring() {
            // given
            Interview interview = Interview.builder()
                    .position(Position.BACKEND)
                    .level(InterviewLevel.JUNIOR)
                    .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                    .durationMinutes(30)
                    .build();

            // when & then
            assertThat(interview.getEffectiveTechStack()).isEqualTo(TechStack.JAVA_SPRING);
        }

        @Test
        @DisplayName("techStack이 PYTHON_DJANGO이면 PYTHON_DJANGO를 반환한다")
        void pythonDjango_returnsPythonDjango() {
            // given
            Interview interview = Interview.builder()
                    .position(Position.BACKEND)
                    .level(InterviewLevel.MID)
                    .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                    .durationMinutes(30)
                    .techStack(TechStack.PYTHON_DJANGO)
                    .build();

            // when & then
            assertThat(interview.getEffectiveTechStack()).isEqualTo(TechStack.PYTHON_DJANGO);
        }

        @Test
        @DisplayName("techStack이 null이고 FRONTEND이면 REACT_TS를 반환한다")
        void nullTechStack_frontendPosition_returnsReactTs() {
            // given
            Interview interview = Interview.builder()
                    .position(Position.FRONTEND)
                    .level(InterviewLevel.JUNIOR)
                    .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                    .durationMinutes(30)
                    .build();

            // when & then
            assertThat(interview.getEffectiveTechStack()).isEqualTo(TechStack.REACT_TS);
        }
    }

    @Nested
    @DisplayName("startQuestionGeneration 메서드")
    class StartQuestionGeneration {

        @Test
        @DisplayName("PENDING 상태에서 GENERATING으로 전이된다")
        void startQuestionGeneration_pendingStatus_changestoGenerating() {
            // given
            Interview interview = createDefaultInterview();

            // when
            interview.startQuestionGeneration();

            // then
            assertThat(interview.getQuestionGenerationStatus())
                    .isEqualTo(QuestionGenerationStatus.GENERATING);
        }

        @Test
        @DisplayName("기존 failureReason이 null로 초기화된다")
        void startQuestionGeneration_withFailureReason_clearsFailureReason() {
            // given
            Interview interview = createDefaultInterview();
            ReflectionTestUtils.setField(interview, "questionGenerationStatus", QuestionGenerationStatus.FAILED);
            ReflectionTestUtils.setField(interview, "failureReason", "이전 실패 사유");

            // when
            interview.startQuestionGeneration();

            // then
            assertThat(interview.getFailureReason()).isNull();
            assertThat(interview.getQuestionGenerationStatus())
                    .isEqualTo(QuestionGenerationStatus.GENERATING);
        }

        @Test
        @DisplayName("COMPLETED 상태에서도 GENERATING으로 전이된다")
        void startQuestionGeneration_completedStatus_changesToGenerating() {
            // given
            Interview interview = createDefaultInterview();
            ReflectionTestUtils.setField(interview, "questionGenerationStatus", QuestionGenerationStatus.COMPLETED);

            // when
            interview.startQuestionGeneration();

            // then
            assertThat(interview.getQuestionGenerationStatus())
                    .isEqualTo(QuestionGenerationStatus.GENERATING);
        }
    }

    @Nested
    @DisplayName("completeQuestionGeneration 메서드")
    class CompleteQuestionGeneration {

        @Test
        @DisplayName("GENERATING 상태에서 COMPLETED로 전이된다")
        void completeQuestionGeneration_generatingStatus_changesToCompleted() {
            // given
            Interview interview = createDefaultInterview();
            ReflectionTestUtils.setField(interview, "questionGenerationStatus", QuestionGenerationStatus.GENERATING);

            // when
            interview.completeQuestionGeneration();

            // then
            assertThat(interview.getQuestionGenerationStatus())
                    .isEqualTo(QuestionGenerationStatus.COMPLETED);
        }

        @Test
        @DisplayName("PENDING 상태에서도 COMPLETED로 전이된다")
        void completeQuestionGeneration_pendingStatus_changesToCompleted() {
            // given
            Interview interview = createDefaultInterview();

            // when
            interview.completeQuestionGeneration();

            // then
            assertThat(interview.getQuestionGenerationStatus())
                    .isEqualTo(QuestionGenerationStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("failQuestionGeneration 메서드")
    class FailQuestionGeneration {

        @Test
        @DisplayName("GENERATING 상태에서 FAILED로 전이되고 실패 사유가 저장된다")
        void failQuestionGeneration_generatingStatus_changesToFailedWithReason() {
            // given
            Interview interview = createDefaultInterview();
            ReflectionTestUtils.setField(interview, "questionGenerationStatus", QuestionGenerationStatus.GENERATING);
            String reason = "AI 서비스 타임아웃";

            // when
            interview.failQuestionGeneration(reason);

            // then
            assertThat(interview.getQuestionGenerationStatus())
                    .isEqualTo(QuestionGenerationStatus.FAILED);
            assertThat(interview.getFailureReason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("실패 사유가 null이어도 FAILED로 전이된다")
        void failQuestionGeneration_nullReason_changesToFailedWithNullReason() {
            // given
            Interview interview = createDefaultInterview();
            ReflectionTestUtils.setField(interview, "questionGenerationStatus", QuestionGenerationStatus.GENERATING);

            // when
            interview.failQuestionGeneration(null);

            // then
            assertThat(interview.getQuestionGenerationStatus())
                    .isEqualTo(QuestionGenerationStatus.FAILED);
            assertThat(interview.getFailureReason()).isNull();
        }
    }

    @Nested
    @DisplayName("resetForRetry 메서드")
    class ResetForRetry {

        @Test
        @DisplayName("FAILED 상태에서 PENDING으로 전이되고 failureReason이 초기화된다")
        void resetForRetry_failedStatus_changesToPendingAndClearsReason() {
            // given
            Interview interview = createDefaultInterview();
            ReflectionTestUtils.setField(interview, "questionGenerationStatus", QuestionGenerationStatus.FAILED);
            ReflectionTestUtils.setField(interview, "failureReason", "이전 실패 사유");

            // when
            interview.resetForRetry();

            // then
            assertThat(interview.getQuestionGenerationStatus())
                    .isEqualTo(QuestionGenerationStatus.PENDING);
            assertThat(interview.getFailureReason()).isNull();
        }

        @Test
        @DisplayName("GENERATING 상태에서도 PENDING으로 전이된다")
        void resetForRetry_generatingStatus_changesToPending() {
            // given
            Interview interview = createDefaultInterview();
            ReflectionTestUtils.setField(interview, "questionGenerationStatus", QuestionGenerationStatus.GENERATING);

            // when
            interview.resetForRetry();

            // then
            assertThat(interview.getQuestionGenerationStatus())
                    .isEqualTo(QuestionGenerationStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("updateStatus 메서드")
    class UpdateStatus {

        @Test
        @DisplayName("READY에서 IN_PROGRESS로 전이에 성공한다")
        void updateStatus_readyToInProgress_succeeds() {
            // given
            Interview interview = createDefaultInterview();

            // when
            interview.updateStatus(InterviewStatus.IN_PROGRESS);

            // then
            assertThat(interview.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("IN_PROGRESS에서 COMPLETED로 전이에 성공한다")
        void updateStatus_inProgressToCompleted_succeeds() {
            // given
            Interview interview = createDefaultInterview();
            ReflectionTestUtils.setField(interview, "status", InterviewStatus.IN_PROGRESS);

            // when
            interview.updateStatus(InterviewStatus.COMPLETED);

            // then
            assertThat(interview.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
        }

        @Test
        @DisplayName("READY에서 COMPLETED로 전이하면 IllegalStateException이 발생한다")
        void updateStatus_readyToCompleted_throwsException() {
            // given
            Interview interview = createDefaultInterview();

            // when & then
            assertThatThrownBy(() -> interview.updateStatus(InterviewStatus.COMPLETED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("READY")
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("COMPLETED에서 IN_PROGRESS로 전이하면 IllegalStateException이 발생한다")
        void updateStatus_completedToInProgress_throwsException() {
            // given
            Interview interview = createDefaultInterview();
            ReflectionTestUtils.setField(interview, "status", InterviewStatus.COMPLETED);

            // when & then
            assertThatThrownBy(() -> interview.updateStatus(InterviewStatus.IN_PROGRESS))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("COMPLETED")
                    .hasMessageContaining("IN_PROGRESS");
        }

        @Test
        @DisplayName("COMPLETED에서 READY로 전이하면 IllegalStateException이 발생한다")
        void updateStatus_completedToReady_throwsException() {
            // given
            Interview interview = createDefaultInterview();
            ReflectionTestUtils.setField(interview, "status", InterviewStatus.COMPLETED);

            // when & then
            assertThatThrownBy(() -> interview.updateStatus(InterviewStatus.READY))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("IN_PROGRESS에서 READY로 전이하면 IllegalStateException이 발생한다")
        void updateStatus_inProgressToReady_throwsException() {
            // given
            Interview interview = createDefaultInterview();
            ReflectionTestUtils.setField(interview, "status", InterviewStatus.IN_PROGRESS);

            // when & then
            assertThatThrownBy(() -> interview.updateStatus(InterviewStatus.READY))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("validateOwner 메서드")
    class ValidateOwner {

        @Test
        @DisplayName("소유자 ID가 일치하면 예외가 발생하지 않는다")
        void validateOwner_matchingUserId_doesNotThrow() {
            // given
            Interview interview = createDefaultInterview();
            ReflectionTestUtils.setField(interview, "userId", 1L);

            // when & then
            interview.validateOwner(1L);
        }

        @Test
        @DisplayName("소유자 ID가 불일치하면 BusinessException이 발생한다")
        void validateOwner_differentUserId_throwsBusinessException() {
            // given
            Interview interview = createDefaultInterview();
            ReflectionTestUtils.setField(interview, "userId", 1L);

            // when & then
            assertThatThrownBy(() -> interview.validateOwner(999L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("userId가 null이면 어떤 사용자도 예외가 발생하지 않는다")
        void validateOwner_nullUserId_doesNotThrow() {
            // given
            Interview interview = createDefaultInterview();
            ReflectionTestUtils.setField(interview, "userId", null);

            // when & then
            interview.validateOwner(999L);
        }
    }

    @Nested
    @DisplayName("recordRetryAttempt 메서드")
    class RecordRetryAttempt {

        @Test
        @DisplayName("최초 호출 시 카운터가 1로 증가하고 시각이 기록된다")
        void recordRetryAttempt_initial_incrementsCounterAndSetsTimestamp() {
            // given
            Interview interview = createDefaultInterview();
            assertThat(interview.getQuestionGenRetryCount()).isZero();
            assertThat(interview.getQuestionGenLastRetriedAt()).isNull();

            // when
            interview.recordRetryAttempt();

            // then
            assertThat(interview.getQuestionGenRetryCount()).isEqualTo(1);
            assertThat(interview.getQuestionGenLastRetriedAt()).isNotNull();
        }

        @Test
        @DisplayName("두 번 연속 호출하면 카운터가 2로 증가한다")
        void recordRetryAttempt_calledTwice_incrementsToTwo() {
            // given
            Interview interview = createDefaultInterview();

            // when
            interview.recordRetryAttempt();
            interview.recordRetryAttempt();

            // then
            assertThat(interview.getQuestionGenRetryCount()).isEqualTo(2);
            assertThat(interview.getQuestionGenLastRetriedAt()).isNotNull();
        }
    }

    private Interview createDefaultInterview() {
        return Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .techStack(TechStack.JAVA_SPRING)
                .build();
    }
}
