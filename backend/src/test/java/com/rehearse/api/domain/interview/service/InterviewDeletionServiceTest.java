package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.questionset.repository.QuestionSetAnalysisRepository;
import com.rehearse.api.domain.feedback.repository.QuestionSetFeedbackRepository;
import com.rehearse.api.domain.feedback.repository.TimestampFeedbackRepository;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.question.repository.QuestionAnswerRepository;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewDeletionService - 면접 세션 삭제")
class InterviewDeletionServiceTest {

    @InjectMocks
    private InterviewDeletionService interviewDeletionService;

    @Mock
    private InterviewFinder interviewFinder;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Mock
    private QuestionAnswerRepository questionAnswerRepository;

    @Mock
    private TimestampFeedbackRepository timestampFeedbackRepository;

    @Mock
    private QuestionSetFeedbackRepository questionSetFeedbackRepository;

    @Mock
    private QuestionSetAnalysisRepository questionSetAnalysisRepository;

    @Nested
    @DisplayName("deleteInterview 메서드")
    class DeleteInterview {

        @Test
        @DisplayName("삭제 가능한 상태의 면접 세션을 정상적으로 삭제한다")
        void deleteInterview_success() {
            // given
            Interview interview = createMockInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);
            given(questionSetRepository.findByInterviewIdOrderByOrderIndex(1L))
                    .willReturn(Collections.emptyList());

            // when
            interviewDeletionService.deleteInterview(1L, 1L);

            // then
            then(questionAnswerRepository).should().deleteAllByInterviewId(1L);
            then(timestampFeedbackRepository).should().deleteAllByInterviewId(1L);
            then(questionSetFeedbackRepository).should().deleteAllByInterviewId(1L);
            then(questionSetAnalysisRepository).should().deleteAllByInterviewId(1L);
            then(questionSetRepository).should().deleteAll(Collections.emptyList());
            then(interviewRepository).should().delete(interview);
        }

        @Test
        @DisplayName("삭제 가능한 상태의 면접 세션을 하위 엔티티와 함께 삭제한다")
        void deleteInterview_withQuestionSets_success() {
            // given
            Interview interview = createMockInterview();
            QuestionSet questionSet = mock(QuestionSet.class);
            List<QuestionSet> questionSets = List.of(questionSet);

            given(interviewFinder.findById(1L)).willReturn(interview);
            given(questionSetRepository.findByInterviewIdOrderByOrderIndex(1L)).willReturn(questionSets);

            // when
            interviewDeletionService.deleteInterview(1L, 1L);

            // then
            then(questionAnswerRepository).should().deleteAllByInterviewId(1L);
            then(timestampFeedbackRepository).should().deleteAllByInterviewId(1L);
            then(questionSetFeedbackRepository).should().deleteAllByInterviewId(1L);
            then(questionSetAnalysisRepository).should().deleteAllByInterviewId(1L);
            then(questionSetRepository).should().deleteAll(questionSets);
            then(interviewRepository).should().delete(interview);
        }

        @Test
        @DisplayName("COMPLETED 상태의 면접 세션도 정상적으로 삭제한다")
        void deleteInterview_completedStatus_success() {
            // given
            Interview interview = createMockInterview();
            ReflectionTestUtils.setField(interview, "status",
                    com.rehearse.api.domain.interview.entity.InterviewStatus.COMPLETED);
            given(interviewFinder.findById(1L)).willReturn(interview);
            given(questionSetRepository.findByInterviewIdOrderByOrderIndex(1L))
                    .willReturn(Collections.emptyList());

            // when
            interviewDeletionService.deleteInterview(1L, 1L);

            // then
            then(questionAnswerRepository).should().deleteAllByInterviewId(1L);
            then(interviewRepository).should().delete(interview);
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
}
