package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.QuestionCategory;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetFeedback;
import com.rehearse.api.domain.questionset.repository.QuestionSetFeedbackRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private QuestionSetFeedbackRepository feedbackRepository;

    // ─────────────────────────────────────────────────────────────
    // checkAndCompleteInterviews
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("checkAndCompleteInterviews: 모든 질문세트가 COMPLETED이면 면접을 COMPLETED로 전이한다")
    void checkAndCompleteInterviews_모든질문세트완료시_면접COMPLETED() {
        // given
        Interview interview = createInProgressInterview(1L);
        QuestionSet qs1 = createQuestionSet(10L, interview, AnalysisStatus.COMPLETED);
        QuestionSet qs2 = createQuestionSet(11L, interview, AnalysisStatus.COMPLETED);

        given(interviewRepository.findByStatus(InterviewStatus.IN_PROGRESS))
                .willReturn(List.of(interview));
        given(questionSetRepository.countByInterviewId(1L)).willReturn(2L);
        given(questionSetRepository.countByInterviewIdAndAnalysisStatus(1L, AnalysisStatus.COMPLETED))
                .willReturn(2L);
        given(questionSetRepository.countByInterviewIdAndAnalysisStatus(1L, AnalysisStatus.SKIPPED))
                .willReturn(0L);
        given(questionSetRepository.findByInterviewIdOrderByOrderIndex(1L))
                .willReturn(List.of(qs1, qs2));

        QuestionSetFeedback fb1 = createFeedback(qs1, 80);
        QuestionSetFeedback fb2 = createFeedback(qs2, 60);
        given(feedbackRepository.findByQuestionSetIdIn(List.of(10L, 11L)))
                .willReturn(List.of(fb1, fb2));

        // when
        interviewCompletionService.checkAndCompleteInterviews();

        // then
        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(interview.getOverallScore()).isEqualTo(70); // (80+60)/2
        assertThat(interview.getOverallComment()).contains("분석 완료");
    }

    @Test
    @DisplayName("checkAndCompleteInterviews: 일부 질문세트만 완료됐을 때 면접 상태를 유지한다")
    void checkAndCompleteInterviews_일부만완료시_상태유지() {
        // given
        Interview interview = createInProgressInterview(1L);

        given(interviewRepository.findByStatus(InterviewStatus.IN_PROGRESS))
                .willReturn(List.of(interview));
        given(questionSetRepository.countByInterviewId(1L)).willReturn(2L);
        given(questionSetRepository.countByInterviewIdAndAnalysisStatus(1L, AnalysisStatus.COMPLETED))
                .willReturn(1L);
        given(questionSetRepository.countByInterviewIdAndAnalysisStatus(1L, AnalysisStatus.SKIPPED))
                .willReturn(0L);

        // when
        interviewCompletionService.checkAndCompleteInterviews();

        // then
        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
        then(feedbackRepository).should(never()).findByQuestionSetIdIn(anyList());
    }

    @Test
    @DisplayName("checkAndCompleteInterviews: 질문세트가 0개이면 면접 완료 처리를 건너뛴다")
    void checkAndCompleteInterviews_질문세트0개시_건너뜀() {
        // given
        Interview interview = createInProgressInterview(1L);

        given(interviewRepository.findByStatus(InterviewStatus.IN_PROGRESS))
                .willReturn(List.of(interview));
        given(questionSetRepository.countByInterviewId(1L)).willReturn(0L);

        // when
        interviewCompletionService.checkAndCompleteInterviews();

        // then
        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
        then(questionSetRepository).should(never())
                .countByInterviewIdAndAnalysisStatus(anyLong(), eq(AnalysisStatus.COMPLETED));
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
        then(questionSetRepository).should(never()).countByInterviewId(anyLong());
    }

    // ─────────────────────────────────────────────────────────────
    // calculateOverallScore (배치 쿼리 경로 검증)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("calculateOverallScore: findByQuestionSetIdIn 배치 쿼리로 평균 점수를 계산한다")
    void calculateOverallScore_배치쿼리로평균점수계산() {
        // given
        Interview interview = createInProgressInterview(2L);
        QuestionSet qs1 = createQuestionSet(20L, interview, AnalysisStatus.COMPLETED);
        QuestionSet qs2 = createQuestionSet(21L, interview, AnalysisStatus.COMPLETED);
        QuestionSet qs3 = createQuestionSet(22L, interview, AnalysisStatus.COMPLETED);

        given(interviewRepository.findByStatus(InterviewStatus.IN_PROGRESS))
                .willReturn(List.of(interview));
        given(questionSetRepository.countByInterviewId(2L)).willReturn(3L);
        given(questionSetRepository.countByInterviewIdAndAnalysisStatus(2L, AnalysisStatus.COMPLETED))
                .willReturn(3L);
        given(questionSetRepository.countByInterviewIdAndAnalysisStatus(2L, AnalysisStatus.SKIPPED))
                .willReturn(0L);
        given(questionSetRepository.findByInterviewIdOrderByOrderIndex(2L))
                .willReturn(List.of(qs1, qs2, qs3));

        QuestionSetFeedback fb1 = createFeedback(qs1, 90);
        QuestionSetFeedback fb2 = createFeedback(qs2, 75);
        QuestionSetFeedback fb3 = createFeedback(qs3, 60);
        given(feedbackRepository.findByQuestionSetIdIn(List.of(20L, 21L, 22L)))
                .willReturn(List.of(fb1, fb2, fb3));

        // when
        interviewCompletionService.checkAndCompleteInterviews();

        // then — (90+75+60)/3 = 75
        assertThat(interview.getOverallScore()).isEqualTo(75);
        then(feedbackRepository).should().findByQuestionSetIdIn(List.of(20L, 21L, 22L));
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

    private QuestionSet createQuestionSet(Long id, Interview interview) {
        return createQuestionSet(id, interview, AnalysisStatus.PENDING);
    }

    private QuestionSet createQuestionSet(Long id, Interview interview, AnalysisStatus status) {
        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category(QuestionCategory.CS)
                .orderIndex(id.intValue())
                .build();
        ReflectionTestUtils.setField(qs, "id", id);
        ReflectionTestUtils.setField(qs, "analysisStatus", status);
        return qs;
    }

    private QuestionSetFeedback createFeedback(QuestionSet qs, int score) {
        return QuestionSetFeedback.builder()
                .questionSet(qs)
                .questionSetScore(score)
                .questionSetComment("피드백 코멘트")
                .build();
    }
}
