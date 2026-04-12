package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetAnalysis;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class InterviewCompletionServiceTest {

    @InjectMocks
    private InterviewCompletionService interviewCompletionService;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private QuestionSetRepository questionSetRepository;

    // ─────────────────────────────────────────────────────────────
    // checkAndCompleteInterviews
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("checkAndCompleteInterviews: 모든 질문세트가 COMPLETED이면 면접을 COMPLETED로 전이한다")
    void checkAndCompleteInterviews_모든질문세트완료시_면접COMPLETED() {
        // given
        Interview interview = createInProgressInterview(1L);
        QuestionSet qs1 = createQuestionSetWithAnalysis(10L, interview, AnalysisStatus.COMPLETED);
        QuestionSet qs2 = createQuestionSetWithAnalysis(11L, interview, AnalysisStatus.COMPLETED);

        given(interviewRepository.findByStatus(InterviewStatus.IN_PROGRESS))
                .willReturn(List.of(interview));
        given(questionSetRepository.findByInterviewIdOrderByOrderIndex(1L))
                .willReturn(List.of(qs1, qs2));
        given(interviewRepository.findById(1L))
                .willReturn(java.util.Optional.of(interview));

        // when
        interviewCompletionService.checkAndCompleteInterviews();

        // then
        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(interview.getOverallComment()).contains("완료");
    }

    @Test
    @DisplayName("checkAndCompleteInterviews: 일부 질문세트만 완료됐을 때 면접 상태를 유지한다")
    void checkAndCompleteInterviews_일부만완료시_상태유지() {
        // given
        Interview interview = createInProgressInterview(1L);
        QuestionSet qs1 = createQuestionSetWithAnalysis(10L, interview, AnalysisStatus.COMPLETED);
        QuestionSet qs2 = createQuestionSetWithAnalysis(11L, interview, AnalysisStatus.ANALYZING);

        given(interviewRepository.findByStatus(InterviewStatus.IN_PROGRESS))
                .willReturn(List.of(interview));
        given(questionSetRepository.findByInterviewIdOrderByOrderIndex(1L))
                .willReturn(List.of(qs1, qs2));

        // when
        interviewCompletionService.checkAndCompleteInterviews();

        // then
        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("checkAndCompleteInterviews: 질문세트가 0개이면 면접 완료 처리를 건너뛴다")
    void checkAndCompleteInterviews_질문세트0개시_건너뜀() {
        // given
        Interview interview = createInProgressInterview(1L);

        given(interviewRepository.findByStatus(InterviewStatus.IN_PROGRESS))
                .willReturn(List.of(interview));
        given(questionSetRepository.findByInterviewIdOrderByOrderIndex(1L))
                .willReturn(List.of());

        // when
        interviewCompletionService.checkAndCompleteInterviews();

        // then
        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("checkAndCompleteInterviews: IN_PROGRESS 면접이 없으면 아무 동작도 하지 않는다")
    void checkAndCompleteInterviews_진행중면접없을때_아무동작없음() {
        // given
        given(interviewRepository.findByStatus(InterviewStatus.IN_PROGRESS))
                .willReturn(List.of());

        // when
        interviewCompletionService.checkAndCompleteInterviews();

        // then
        then(questionSetRepository).should(never()).findByInterviewIdOrderByOrderIndex(anyLong());
    }

    @Test
    @DisplayName("checkAndCompleteInterviews: COMPLETED + PARTIAL 조합이면 면접을 완료 처리한다")
    void checkAndCompleteInterviews_완료및부분완료시_면접COMPLETED() {
        // given
        Interview interview = createInProgressInterview(2L);
        QuestionSet qs1 = createQuestionSetWithAnalysis(20L, interview, AnalysisStatus.COMPLETED);
        QuestionSet qs2 = createQuestionSetWithAnalysis(21L, interview, AnalysisStatus.COMPLETED);
        QuestionSet qs3 = createQuestionSetWithAnalysis(22L, interview, AnalysisStatus.COMPLETED);

        given(interviewRepository.findByStatus(InterviewStatus.IN_PROGRESS))
                .willReturn(List.of(interview));
        given(questionSetRepository.findByInterviewIdOrderByOrderIndex(2L))
                .willReturn(List.of(qs1, qs2, qs3));
        given(interviewRepository.findById(2L))
                .willReturn(java.util.Optional.of(interview));

        // when
        interviewCompletionService.checkAndCompleteInterviews();

        // then
        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(interview.getOverallComment()).contains("완료");
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────

    private Interview createInProgressInterview(Long id) {
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .durationMinutes(30)
                .build();
        ReflectionTestUtils.setField(interview, "id", id);
        interview.updateStatus(InterviewStatus.IN_PROGRESS);
        return interview;
    }

    private QuestionSet createQuestionSetWithAnalysis(Long id, Interview interview, AnalysisStatus status) {
        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category("CS_FUNDAMENTAL")
                .orderIndex(id.intValue())
                .build();
        ReflectionTestUtils.setField(qs, "id", id);

        QuestionSetAnalysis analysis = QuestionSetAnalysis.builder()
                .questionSet(qs)
                .build();
        if (status != AnalysisStatus.PENDING) {
            ReflectionTestUtils.setField(analysis, "analysisStatus", status);
        }
        ReflectionTestUtils.setField(qs, "analysis", analysis);
        return qs;
    }
}
