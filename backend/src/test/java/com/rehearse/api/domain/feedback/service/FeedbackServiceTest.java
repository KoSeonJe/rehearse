package com.rehearse.api.domain.feedback.service;

import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.session.event.DeliveryEnrichmentRequestedEvent;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.question.entity.AnalysisStatus;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionSetAnalysis;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.question.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.question.repository.QuestionSetAnalysisRepository;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @InjectMocks
    private FeedbackService feedbackService;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Mock
    private QuestionSetAnalysisRepository analysisRepository;

    @Mock
    private QuestionSetFeedbackPersister feedbackPersister;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("saveFeedback_completes_analysis_when_verbal_and_nonverbal_both_true")
    void saveFeedback_completes_analysis_when_both_completed() {
        // given
        QuestionSet questionSet = createQuestionSet(1L);
        QuestionSetAnalysis analysis = createAnalysis(1L, AnalysisStatus.FINALIZING);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));

        SaveFeedbackRequest request = new SaveFeedbackRequest();
        ReflectionTestUtils.setField(request, "questionSetComment", "전반적으로 좋은 답변입니다.");
        ReflectionTestUtils.setField(request, "timestampFeedbacks", null);
        ReflectionTestUtils.setField(request, "verbalCompleted", true);
        ReflectionTestUtils.setField(request, "nonverbalCompleted", true);

        // when
        feedbackService.saveFeedback(1L, request);

        // then
        assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        then(feedbackPersister).should().persist(any(QuestionSet.class), any(SaveFeedbackRequest.class));
    }

    @Test
    @DisplayName("saveFeedback_marks_analysis_partial_when_only_verbal_completed")
    void saveFeedback_partial_when_only_verbal_completed() {
        // given
        QuestionSet questionSet = createQuestionSet(1L);
        QuestionSetAnalysis analysis = createAnalysis(1L, AnalysisStatus.FINALIZING);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));

        SaveFeedbackRequest request = new SaveFeedbackRequest();
        ReflectionTestUtils.setField(request, "questionSetComment", "비언어 분석 실패");
        ReflectionTestUtils.setField(request, "timestampFeedbacks", null);
        ReflectionTestUtils.setField(request, "verbalCompleted", true);
        ReflectionTestUtils.setField(request, "nonverbalCompleted", false);

        // when
        feedbackService.saveFeedback(1L, request);

        // then
        assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.PARTIAL);
        assertThat(analysis.isVerbalCompleted()).isTrue();
        assertThat(analysis.isNonverbalCompleted()).isFalse();
    }

    @Test
    @DisplayName("saveFeedback_throws_BusinessException_when_questionSet_not_found")
    void saveFeedback_throws_when_questionSet_not_found() {
        given(questionSetRepository.findById(999L)).willReturn(Optional.empty());

        SaveFeedbackRequest request = new SaveFeedbackRequest();
        ReflectionTestUtils.setField(request, "verbalCompleted", true);
        ReflectionTestUtils.setField(request, "nonverbalCompleted", true);

        assertThatThrownBy(() -> feedbackService.saveFeedback(999L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo(QuestionSetErrorCode.NOT_FOUND.getCode());
                });
    }

    @Test
    @DisplayName("saveFeedback_throws_BusinessException_when_analysis_not_found")
    void saveFeedback_throws_when_analysis_not_found() {
        QuestionSet questionSet = createQuestionSet(1L);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.empty());

        SaveFeedbackRequest request = new SaveFeedbackRequest();
        ReflectionTestUtils.setField(request, "verbalCompleted", true);
        ReflectionTestUtils.setField(request, "nonverbalCompleted", true);

        assertThatThrownBy(() -> feedbackService.saveFeedback(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo(QuestionSetErrorCode.NOT_FOUND.getCode());
                });
    }

    @Test
    @DisplayName("saveFeedback_모든_QuestionSet_완료_시_DeliveryEnrichmentRequestedEvent_발행")
    void saveFeedback_publishes_delivery_enrichment_event_when_all_settled() {
        // given
        QuestionSet questionSet = createQuestionSet(1L);
        QuestionSetAnalysis analysis = createAnalysis(1L, AnalysisStatus.FINALIZING);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));
        given(analysisRepository.findAllByQuestionSetInterviewId(any())).willReturn(List.of(analysis));

        SaveFeedbackRequest request = createRequest(true, true);

        // when
        feedbackService.saveFeedback(1L, request);

        // then
        then(eventPublisher).should().publishEvent(any(DeliveryEnrichmentRequestedEvent.class));
    }

    @Test
    @DisplayName("saveFeedback_진행중_QuestionSet_존재_시_이벤트_미발행")
    void saveFeedback_does_not_publish_event_when_analysis_still_in_progress() {
        // given
        QuestionSet questionSet = createQuestionSet(1L);
        QuestionSetAnalysis analysis = createAnalysis(1L, AnalysisStatus.FINALIZING);
        QuestionSetAnalysis otherAnalysis = createAnalysis(2L, AnalysisStatus.ANALYZING);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));
        given(analysisRepository.findAllByQuestionSetInterviewId(any())).willReturn(List.of(analysis, otherAnalysis));

        SaveFeedbackRequest request = createRequest(true, true);

        // when
        feedbackService.saveFeedback(1L, request);

        // then
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("saveFeedback_QuestionSet_0개_시_이벤트_미발행")
    void saveFeedback_does_not_publish_event_when_no_question_sets() {
        // given
        QuestionSet questionSet = createQuestionSet(1L);
        QuestionSetAnalysis analysis = createAnalysis(1L, AnalysisStatus.FINALIZING);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));
        given(analysisRepository.findAllByQuestionSetInterviewId(any())).willReturn(List.of());

        SaveFeedbackRequest request = createRequest(true, true);

        // when
        feedbackService.saveFeedback(1L, request);

        // then
        then(eventPublisher).should(never()).publishEvent(any());
    }

    // ----------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------

    private SaveFeedbackRequest createRequest(boolean verbal, boolean nonverbal) {
        SaveFeedbackRequest request = new SaveFeedbackRequest();
        ReflectionTestUtils.setField(request, "questionSetComment", "테스트 코멘트");
        ReflectionTestUtils.setField(request, "timestampFeedbacks", null);
        ReflectionTestUtils.setField(request, "verbalCompleted", verbal);
        ReflectionTestUtils.setField(request, "nonverbalCompleted", nonverbal);
        return request;
    }

    private QuestionSet createQuestionSet(Long id) {
        Interview interview = mock(Interview.class);
        lenient().when(interview.getId()).thenReturn(100L);
        QuestionSet questionSet = QuestionSet.builder()
                .interview(interview)
                .category(InterviewType.CS_FUNDAMENTAL)
                .orderIndex(1)
                .build();
        ReflectionTestUtils.setField(questionSet, "id", id);
        return questionSet;
    }

    private QuestionSetAnalysis createAnalysis(Long questionSetId, AnalysisStatus targetStatus) {
        QuestionSet questionSet = createQuestionSet(questionSetId);
        QuestionSetAnalysis analysis = QuestionSetAnalysis.builder()
                .questionSet(questionSet)
                .build();
        if (targetStatus != AnalysisStatus.PENDING) {
            ReflectionTestUtils.setField(analysis, "analysisStatus", targetStatus);
        }
        return analysis;
    }
}
