package com.rehearse.api.domain.questionset.service;

import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.questionset.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.questionset.dto.UpdateProgressRequest;
import com.rehearse.api.domain.questionset.entity.*;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.repository.*;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.aws.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class InternalQuestionSetServiceTest {

    @InjectMocks
    private InternalQuestionSetService internalQuestionSetService;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Mock
    private QuestionAnswerRepository answerRepository;

    @Mock
    private QuestionSetFeedbackRepository feedbackRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private S3Service s3Service;

    // ----------------------------------------------------------------
    // updateProgress
    // ----------------------------------------------------------------

    @Test
    @DisplayName("updateProgress: PENDING_UPLOAD 상태에서 ANALYZING으로 전이되고 progress가 업데이트된다")
    void updateProgress_transitionsToAnalyzing() {
        // given
        QuestionSet questionSet = createQuestionSet(1L, AnalysisStatus.PENDING_UPLOAD);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));

        UpdateProgressRequest request = new UpdateProgressRequest();
        ReflectionTestUtils.setField(request, "progress", AnalysisProgress.EXTRACTING);

        // when
        internalQuestionSetService.updateProgress(1L, request);

        // then
        assertThat(questionSet.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZING);
        assertThat(questionSet.getAnalysisProgress()).isEqualTo(AnalysisProgress.EXTRACTING);
    }

    @Test
    @DisplayName("updateProgress: 이미 ANALYZING 상태이면 상태 전이 없이 progress만 업데이트된다")
    void updateProgress_alreadyAnalyzing_onlyProgressUpdated() {
        // given
        QuestionSet questionSet = createQuestionSet(1L, AnalysisStatus.ANALYZING);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));

        UpdateProgressRequest request = new UpdateProgressRequest();
        ReflectionTestUtils.setField(request, "progress", AnalysisProgress.VERBAL_ANALYZING);

        // when
        internalQuestionSetService.updateProgress(1L, request);

        // then
        assertThat(questionSet.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZING);
        assertThat(questionSet.getAnalysisProgress()).isEqualTo(AnalysisProgress.VERBAL_ANALYZING);
    }

    @Test
    @DisplayName("updateProgress: 존재하지 않는 질문세트 ID로 요청하면 BusinessException이 발생한다")
    void updateProgress_questionSetNotFound() {
        // given
        given(questionSetRepository.findById(999L)).willReturn(Optional.empty());

        UpdateProgressRequest request = new UpdateProgressRequest();
        ReflectionTestUtils.setField(request, "progress", AnalysisProgress.EXTRACTING);

        // when & then
        assertThatThrownBy(() -> internalQuestionSetService.updateProgress(999L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo(QuestionSetErrorCode.NOT_FOUND.getCode());
                });
    }

    // ----------------------------------------------------------------
    // getAnswers
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getAnswers: 질문세트 ID에 해당하는 답변 목록이 반환된다")
    void getAnswers_success() {
        // given
        Question question = createQuestion(10L);
        QuestionAnswer answer = QuestionAnswer.builder()
                .question(question)
                .startMs(0L)
                .endMs(5000L)
                .build();
        ReflectionTestUtils.setField(answer, "id", 1L);

        given(answerRepository.findByQuestionSetIdWithQuestion(1L)).willReturn(List.of(answer));

        // when
        List<QuestionAnswer> answers = internalQuestionSetService.getAnswers(1L);

        // then
        assertThat(answers).hasSize(1);
        assertThat(answers.get(0).getStartMs()).isEqualTo(0L);
        assertThat(answers.get(0).getEndMs()).isEqualTo(5000L);
        assertThat(answers.get(0).getQuestion().getId()).isEqualTo(10L);
    }

    // ----------------------------------------------------------------
    // saveFeedback
    // ----------------------------------------------------------------

    @Test
    @DisplayName("saveFeedback: 피드백과 타임스탬프 피드백이 저장되고 상태가 COMPLETED로 변경된다")
    void saveFeedback_withTimestampFeedbacks_success() {
        // given
        QuestionSet questionSet = createQuestionSet(1L, AnalysisStatus.ANALYZING);
        Question question = createQuestion(10L);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(questionRepository.findById(10L)).willReturn(Optional.of(question));
        given(feedbackRepository.save(any(QuestionSetFeedback.class)))
                .willAnswer(inv -> inv.getArgument(0));

        SaveFeedbackRequest.TimestampFeedbackItem item = new SaveFeedbackRequest.TimestampFeedbackItem();
        ReflectionTestUtils.setField(item, "questionId", 10L);
        ReflectionTestUtils.setField(item, "startMs", 0L);
        ReflectionTestUtils.setField(item, "endMs", 5000L);
        ReflectionTestUtils.setField(item, "verbalScore", 80);
        ReflectionTestUtils.setField(item, "verbalComment", "명확한 설명");

        SaveFeedbackRequest request = new SaveFeedbackRequest();
        ReflectionTestUtils.setField(request, "questionSetScore", 85);
        ReflectionTestUtils.setField(request, "questionSetComment", "전반적으로 좋은 답변입니다.");
        ReflectionTestUtils.setField(request, "timestampFeedbacks", List.of(item));

        // when
        internalQuestionSetService.saveFeedback(1L, request);

        // then
        assertThat(questionSet.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(questionSet.getAnalysisProgress()).isEqualTo(AnalysisProgress.FINALIZING);
        then(feedbackRepository).should().save(any(QuestionSetFeedback.class));
    }

    @Test
    @DisplayName("saveFeedback: 타임스탬프 피드백 없이도 정상적으로 저장된다")
    void saveFeedback_withoutTimestampFeedbacks_success() {
        // given
        QuestionSet questionSet = createQuestionSet(1L, AnalysisStatus.ANALYZING);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(feedbackRepository.save(any(QuestionSetFeedback.class)))
                .willAnswer(inv -> inv.getArgument(0));

        SaveFeedbackRequest request = new SaveFeedbackRequest();
        ReflectionTestUtils.setField(request, "questionSetScore", 70);
        ReflectionTestUtils.setField(request, "questionSetComment", "개선이 필요합니다.");
        ReflectionTestUtils.setField(request, "timestampFeedbacks", null);

        // when
        internalQuestionSetService.saveFeedback(1L, request);

        // then
        assertThat(questionSet.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        then(feedbackRepository).should().save(any(QuestionSetFeedback.class));
    }

    // ----------------------------------------------------------------
    // retryAnalysis
    // ----------------------------------------------------------------

    @Test
    @DisplayName("retryAnalysis: FAILED 상태에서 ANALYZING으로 전이되고 progress가 STARTED로 설정된다")
    void retryAnalysis_success() {
        // given
        QuestionSet questionSet = createQuestionSet(1L, AnalysisStatus.FAILED);
        ReflectionTestUtils.setField(questionSet, "analysisProgress", AnalysisProgress.FAILED);

        FileMetadata fileMetadata = mock(FileMetadata.class);
        given(fileMetadata.getS3Key()).willReturn("videos/100/qs_1.webm");
        ReflectionTestUtils.setField(questionSet, "fileMetadata", fileMetadata);

        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));

        // TransactionSynchronizationManager 활성화 (afterCommit 콜백 등록용)
        TransactionSynchronizationManager.initSynchronization();
        try {
            // when
            internalQuestionSetService.retryAnalysis(1L);

            // then
            assertThat(questionSet.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZING);
            assertThat(questionSet.getAnalysisProgress()).isEqualTo(AnalysisProgress.STARTED);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("retryAnalysis: FAILED가 아닌 상태에서 호출하면 BusinessException이 발생한다")
    void retryAnalysis_notFailedStatus() {
        // given
        QuestionSet questionSet = createQuestionSet(1L, AnalysisStatus.ANALYZING);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));

        // when & then
        assertThatThrownBy(() -> internalQuestionSetService.retryAnalysis(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo(QuestionSetErrorCode.INVALID_ANALYSIS_STATUS_TRANSITION.getCode());
                });
    }

    @Test
    @DisplayName("retryAnalysis: fileMetadata가 없으면 BusinessException이 발생한다")
    void retryAnalysis_fileNotFound() {
        // given
        QuestionSet questionSet = createQuestionSet(1L, AnalysisStatus.FAILED);
        ReflectionTestUtils.setField(questionSet, "fileMetadata", null);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));

        // when & then
        assertThatThrownBy(() -> internalQuestionSetService.retryAnalysis(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo(QuestionSetErrorCode.FILE_NOT_FOUND.getCode());
                });
    }

    @Test
    @DisplayName("retryAnalysis: 존재하지 않는 질문세트 ID로 요청하면 BusinessException이 발생한다")
    void retryAnalysis_questionSetNotFound() {
        // given
        given(questionSetRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> internalQuestionSetService.retryAnalysis(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo(QuestionSetErrorCode.NOT_FOUND.getCode());
                });
    }

    // ----------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------

    private QuestionSet createQuestionSet(Long id, AnalysisStatus status) {
        Interview interview = mock(Interview.class);
        lenient().when(interview.getId()).thenReturn(100L);

        QuestionSet questionSet = QuestionSet.builder()
                .interview(interview)
                .category(QuestionCategory.CS)
                .orderIndex(1)
                .build();
        ReflectionTestUtils.setField(questionSet, "id", id);
        if (status != AnalysisStatus.PENDING) {
            ReflectionTestUtils.setField(questionSet, "analysisStatus", status);
        }
        return questionSet;
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
