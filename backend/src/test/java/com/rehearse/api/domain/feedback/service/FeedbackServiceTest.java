package com.rehearse.api.domain.feedback.service;

import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.repository.QuestionSetFeedbackRepository;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.repository.QuestionRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @InjectMocks
    private FeedbackService feedbackService;

    @Mock
    private QuestionSetFeedbackRepository feedbackRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Mock
    private QuestionSetAnalysisRepository analysisRepository;

    @Mock
    private TimestampFeedbackMapper timestampFeedbackMapper;

    @Test
    @DisplayName("saveFeedback_persists_feedback_and_marks_analysis_completed_when_verbal_and_nonverbal_both_true")
    void saveFeedback_persists_feedback_and_completes_analysis() {
        // given
        QuestionSet questionSet = createQuestionSet(1L);
        QuestionSetAnalysis analysis = createAnalysis(1L, AnalysisStatus.FINALIZING);
        Question question = createQuestion(10L);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));
        given(questionRepository.findById(10L)).willReturn(Optional.of(question));
        given(feedbackRepository.save(any(QuestionSetFeedback.class)))
                .willAnswer(inv -> inv.getArgument(0));

        SaveFeedbackRequest.TimestampFeedbackItem item = new SaveFeedbackRequest.TimestampFeedbackItem();
        ReflectionTestUtils.setField(item, "questionId", 10L);
        ReflectionTestUtils.setField(item, "startMs", 0L);
        ReflectionTestUtils.setField(item, "endMs", 5000L);
        ReflectionTestUtils.setField(item, "eyeContactLevel", "GOOD");
        ReflectionTestUtils.setField(item, "postureLevel", "AVERAGE");
        ReflectionTestUtils.setField(item, "toneConfidenceLevel", "GOOD");

        TimestampFeedback stubTf = TimestampFeedback.builder()
                .startMs(0L)
                .endMs(5000L)
                .isAnalyzed(true)
                .build();
        given(timestampFeedbackMapper.toEntity(eq(item), any())).willReturn(stubTf);

        SaveFeedbackRequest request = new SaveFeedbackRequest();
        ReflectionTestUtils.setField(request, "questionSetComment", "전반적으로 좋은 답변입니다.");
        ReflectionTestUtils.setField(request, "timestampFeedbacks", List.of(item));
        ReflectionTestUtils.setField(request, "verbalCompleted", true);
        ReflectionTestUtils.setField(request, "nonverbalCompleted", true);

        // when
        feedbackService.saveFeedback(1L, request);

        // then
        assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        then(feedbackRepository).should().save(any(QuestionSetFeedback.class));
    }

    @Test
    @DisplayName("saveFeedback_persists_feedback_when_timestampFeedbacks_null")
    void saveFeedback_handles_null_timestampFeedbacks() {
        // given
        QuestionSet questionSet = createQuestionSet(1L);
        QuestionSetAnalysis analysis = createAnalysis(1L, AnalysisStatus.FINALIZING);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));
        given(feedbackRepository.save(any(QuestionSetFeedback.class)))
                .willAnswer(inv -> inv.getArgument(0));

        SaveFeedbackRequest request = new SaveFeedbackRequest();
        ReflectionTestUtils.setField(request, "questionSetComment", "개선이 필요합니다.");
        ReflectionTestUtils.setField(request, "timestampFeedbacks", null);
        ReflectionTestUtils.setField(request, "verbalCompleted", true);
        ReflectionTestUtils.setField(request, "nonverbalCompleted", false);

        // when
        feedbackService.saveFeedback(1L, request);

        // then
        assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.PARTIAL);
        then(feedbackRepository).should().save(any(QuestionSetFeedback.class));
    }

    @Test
    @DisplayName("saveFeedback_marks_analysis_partial_when_verbal_true_and_nonverbal_false")
    void saveFeedback_partial_when_only_verbal_completed() {
        // given
        QuestionSet questionSet = createQuestionSet(1L);
        QuestionSetAnalysis analysis = createAnalysis(1L, AnalysisStatus.FINALIZING);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));
        given(feedbackRepository.save(any(QuestionSetFeedback.class)))
                .willAnswer(inv -> inv.getArgument(0));

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
    @DisplayName("saveFeedback_continues_with_null_question_when_questionId_unknown")
    void saveFeedback_continues_when_questionId_unknown() {
        // given
        QuestionSet questionSet = createQuestionSet(1L);
        QuestionSetAnalysis analysis = createAnalysis(1L, AnalysisStatus.FINALIZING);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));
        given(questionRepository.findById(404L)).willReturn(Optional.empty());
        given(feedbackRepository.save(any(QuestionSetFeedback.class)))
                .willAnswer(inv -> inv.getArgument(0));

        SaveFeedbackRequest.TimestampFeedbackItem item = new SaveFeedbackRequest.TimestampFeedbackItem();
        ReflectionTestUtils.setField(item, "questionId", 404L);
        ReflectionTestUtils.setField(item, "startMs", 0L);
        ReflectionTestUtils.setField(item, "endMs", 1000L);

        TimestampFeedback stubTf = TimestampFeedback.builder()
                .startMs(0L)
                .endMs(1000L)
                .isAnalyzed(true)
                .build();
        given(timestampFeedbackMapper.toEntity(eq(item), eq(null))).willReturn(stubTf);

        SaveFeedbackRequest request = new SaveFeedbackRequest();
        ReflectionTestUtils.setField(request, "questionSetComment", "타임스탬프만");
        ReflectionTestUtils.setField(request, "timestampFeedbacks", List.of(item));
        ReflectionTestUtils.setField(request, "verbalCompleted", true);
        ReflectionTestUtils.setField(request, "nonverbalCompleted", true);

        // when
        feedbackService.saveFeedback(1L, request);

        // then
        assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        then(feedbackRepository).should().save(any(QuestionSetFeedback.class));
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

    private Question createQuestion(Long id) {
        Question question = Question.builder()
                .questionType(QuestionType.MAIN)
                .questionText("Java의 GC 동작 원리를 설명하세요.")
                .orderIndex(1)
                .build();
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }
}
