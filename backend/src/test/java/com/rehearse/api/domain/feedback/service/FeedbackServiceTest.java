package com.rehearse.api.domain.feedback.service;

import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetAnalysis;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.repository.QuestionSetAnalysisRepository;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

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

    // ----------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------

    private QuestionSet createQuestionSet(Long id) {
        Interview interview = mock(Interview.class);
        QuestionSet questionSet = QuestionSet.builder()
                .interview(interview)
                .category(QuestionSetCategory.CS_FUNDAMENTAL)
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
