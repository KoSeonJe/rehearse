package com.rehearse.api.domain.questionset.service;

import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.questionset.dto.UpdateConvertStatusRequest;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.ConvertStatus;
import com.rehearse.api.domain.questionset.entity.QuestionSetAnalysis;
import com.rehearse.api.domain.questionset.exception.AnalysisErrorCode;
import com.rehearse.api.domain.questionset.repository.QuestionSetAnalysisRepository;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionAnswer;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.repository.QuestionAnswerRepository;
import com.rehearse.api.domain.questionset.dto.UpdateProgressRequest;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.aws.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;

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
    private QuestionSetAnalysisRepository analysisRepository;

    @Mock
    private QuestionAnswerRepository answerRepository;

    @Mock
    private S3Service s3Service;

    @Nested
    @DisplayName("updateProgress 메서드")
    class UpdateProgress {

        @Test
        @DisplayName("updateProgress: PENDING_UPLOAD 상태에서 EXTRACTING으로 전이된다")
        void updateProgress_transitionsToExtracting() {
            // given
            QuestionSetAnalysis analysis = createAnalysis(1L, AnalysisStatus.PENDING_UPLOAD);
            given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));

            UpdateProgressRequest request = new UpdateProgressRequest();
            ReflectionTestUtils.setField(request, "status", AnalysisStatus.EXTRACTING);

            // when
            internalQuestionSetService.updateProgress(1L, request);

            // then
            assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.EXTRACTING);
        }

        @Test
        @DisplayName("updateProgress: EXTRACTING 상태에서 ANALYZING으로 전이된다")
        void updateProgress_alreadyExtracting_transitionsToAnalyzing() {
            // given
            QuestionSetAnalysis analysis = createAnalysis(1L, AnalysisStatus.EXTRACTING);
            given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));

            UpdateProgressRequest request = new UpdateProgressRequest();
            ReflectionTestUtils.setField(request, "status", AnalysisStatus.ANALYZING);

            // when
            internalQuestionSetService.updateProgress(1L, request);

            // then
            assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZING);
        }

        @Test
        @DisplayName("updateProgress: 존재하지 않는 질문세트 ID로 요청하면 BusinessException이 발생한다")
        void updateProgress_questionSetNotFound() {
            // given
            given(analysisRepository.findByQuestionSetId(999L)).willReturn(Optional.empty());

            UpdateProgressRequest request = new UpdateProgressRequest();
            ReflectionTestUtils.setField(request, "status", AnalysisStatus.EXTRACTING);

            // when & then
            assertThatThrownBy(() -> internalQuestionSetService.updateProgress(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(be.getCode()).isEqualTo(QuestionSetErrorCode.NOT_FOUND.getCode());
                    });
        }
    }

    @Nested
    @DisplayName("getAnswers 메서드")
    class GetAnswers {

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
    }

    @Nested
    @DisplayName("updateConvertStatus 메서드")
    class UpdateConvertStatus {

        @Test
        @DisplayName("updateConvertStatus: PROCESSING 전이 + streamingS3Key가 FileMetadata에 저장된다")
        void updateConvertStatus_processing_savesStreamingS3Key() {
            // given
            QuestionSet questionSet = createQuestionSet(1L);
            FileMetadata fileMetadata = mock(FileMetadata.class);
            ReflectionTestUtils.setField(questionSet, "fileMetadata", fileMetadata);

            QuestionSetAnalysis analysis = QuestionSetAnalysis.builder()
                    .questionSet(questionSet)
                    .build();
            // PENDING → PROCESSING 전이를 위해 convertStatus는 기본값(PENDING) 유지

            given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));

            UpdateConvertStatusRequest request = new UpdateConvertStatusRequest();
            ReflectionTestUtils.setField(request, "status", ConvertStatus.PROCESSING);
            ReflectionTestUtils.setField(request, "streamingS3Key", "streaming/1/output.m3u8");
            ReflectionTestUtils.setField(request, "failureReason", null);

            // when
            internalQuestionSetService.updateConvertStatus(1L, request);

            // then
            assertThat(analysis.getConvertStatus()).isEqualTo(ConvertStatus.PROCESSING);
            then(fileMetadata).should().updateStreamingS3Key("streaming/1/output.m3u8");
        }

        @Test
        @DisplayName("updateConvertStatus: FAILED 전이 + convertFailureReason이 저장된다")
        void updateConvertStatus_failed_savesConvertFailureReason() {
            // given
            QuestionSetAnalysis analysis = createAnalysis(1L, AnalysisStatus.PENDING);
            // PENDING → FAILED 전이는 ConvertStatus 기준: PENDING → FAILED 허용
            given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));

            UpdateConvertStatusRequest request = new UpdateConvertStatusRequest();
            ReflectionTestUtils.setField(request, "status", ConvertStatus.FAILED);
            ReflectionTestUtils.setField(request, "streamingS3Key", null);
            ReflectionTestUtils.setField(request, "failureReason", "MediaConvert job failed");

            // when
            internalQuestionSetService.updateConvertStatus(1L, request);

            // then
            assertThat(analysis.getConvertStatus()).isEqualTo(ConvertStatus.FAILED);
            assertThat(analysis.getConvertFailureReason()).isEqualTo("MediaConvert job failed");
        }
    }

    @Nested
    @DisplayName("retryAnalysis 메서드")
    class RetryAnalysis {

        @Test
        @DisplayName("retryAnalysis: PARTIAL 상태에서 EXTRACTING으로 재시도 성공한다")
        void retryAnalysis_fromPartial_success() {
            // given
            QuestionSet questionSet = createQuestionSet(1L);
            FileMetadata fileMetadata = mock(FileMetadata.class);
            given(fileMetadata.getS3Key()).willReturn("videos/100/qs_1.webm");
            ReflectionTestUtils.setField(questionSet, "fileMetadata", fileMetadata);

            QuestionSetAnalysis analysis = QuestionSetAnalysis.builder()
                    .questionSet(questionSet)
                    .build();
            ReflectionTestUtils.setField(analysis, "analysisStatus", AnalysisStatus.PARTIAL);

            given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));

            TransactionSynchronizationManager.initSynchronization();
            try {
                // when
                internalQuestionSetService.retryAnalysis(1L);

                // then
                assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.EXTRACTING);
                assertThat(analysis.isVerbalCompleted()).isFalse();
                assertThat(analysis.isNonverbalCompleted()).isFalse();
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("retryAnalysis: FAILED 상태에서 EXTRACTING으로 전이된다")
        void retryAnalysis_success() {
            // given
            QuestionSet questionSet = createQuestionSet(1L);
            FileMetadata fileMetadata = mock(FileMetadata.class);
            given(fileMetadata.getS3Key()).willReturn("videos/100/qs_1.webm");
            ReflectionTestUtils.setField(questionSet, "fileMetadata", fileMetadata);

            // analysis가 참조하는 questionSet에 fileMetadata가 있어야 함
            QuestionSetAnalysis analysis = QuestionSetAnalysis.builder()
                    .questionSet(questionSet)
                    .build();
            ReflectionTestUtils.setField(analysis, "analysisStatus", AnalysisStatus.FAILED);

            given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));

            // TransactionSynchronizationManager 활성화 (afterCommit 콜백 등록용)
            TransactionSynchronizationManager.initSynchronization();
            try {
                // when
                internalQuestionSetService.retryAnalysis(1L);

                // then
                assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.EXTRACTING);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("retryAnalysis: FAILED/PARTIAL이 아닌 상태에서 호출하면 BusinessException이 발생한다")
        void retryAnalysis_notFailedStatus() {
            // given
            QuestionSetAnalysis analysis = createAnalysis(1L, AnalysisStatus.ANALYZING);
            given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));

            // when & then
            assertThatThrownBy(() -> internalQuestionSetService.retryAnalysis(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo(AnalysisErrorCode.INVALID_ANALYSIS_STATUS_TRANSITION.getCode());
                    });
        }

        @Test
        @DisplayName("retryAnalysis: fileMetadata가 없으면 BusinessException이 발생한다")
        void retryAnalysis_fileNotFound() {
            // given
            QuestionSet questionSet = createQuestionSet(1L);
            // fileMetadata는 null (기본값)
            QuestionSetAnalysis analysis = QuestionSetAnalysis.builder()
                    .questionSet(questionSet)
                    .build();
            ReflectionTestUtils.setField(analysis, "analysisStatus", AnalysisStatus.FAILED);
            given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.of(analysis));

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
            given(analysisRepository.findByQuestionSetId(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> internalQuestionSetService.retryAnalysis(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(be.getCode()).isEqualTo(QuestionSetErrorCode.NOT_FOUND.getCode());
                    });
        }
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
        // PENDING → target 으로 ReflectionTestUtils로 강제 설정
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
