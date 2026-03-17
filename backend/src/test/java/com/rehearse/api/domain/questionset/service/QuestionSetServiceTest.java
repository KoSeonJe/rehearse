package com.rehearse.api.domain.questionset.service;

import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.file.entity.FileType;
import com.rehearse.api.domain.file.repository.FileMetadataRepository;
import com.rehearse.api.domain.questionset.dto.*;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionSetServiceTest {

    @InjectMocks
    private QuestionSetService questionSetService;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionSetAnswerRepository answerRepository;

    @Mock
    private QuestionSetFeedbackRepository feedbackRepository;

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private S3Service s3Service;

    // ----------------------------------------------------------------
    // saveAnswers
    // ----------------------------------------------------------------

    @Test
    @DisplayName("saveAnswers: 답변 구간이 저장되고 질문세트 상태가 PENDING_UPLOAD로 변경된다")
    void saveAnswers_success() {
        // given
        QuestionSet questionSet = createQuestionSet(1L, AnalysisStatus.PENDING);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));

        Question question = createQuestion(10L);
        given(questionRepository.findById(10L)).willReturn(Optional.of(question));
        given(answerRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        SaveAnswersRequest request = new SaveAnswersRequest();
        SaveAnswersRequest.AnswerTimestamp timestamp = new SaveAnswersRequest.AnswerTimestamp();
        ReflectionTestUtils.setField(timestamp, "questionId", 10L);
        ReflectionTestUtils.setField(timestamp, "startMs", 0L);
        ReflectionTestUtils.setField(timestamp, "endMs", 5000L);
        ReflectionTestUtils.setField(request, "answers", List.of(timestamp));

        // when
        questionSetService.saveAnswers(1L, request);

        // then
        assertThat(questionSet.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING_UPLOAD);
        then(answerRepository).should().saveAll(anyList());
    }

    @Test
    @DisplayName("saveAnswers: 존재하지 않는 질문세트 ID로 요청하면 BusinessException이 발생한다")
    void saveAnswers_questionSetNotFound() {
        // given
        given(questionSetRepository.findById(999L)).willReturn(Optional.empty());

        SaveAnswersRequest request = new SaveAnswersRequest();
        ReflectionTestUtils.setField(request, "answers", List.of());

        // when & then
        assertThatThrownBy(() -> questionSetService.saveAnswers(999L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo(QuestionSetErrorCode.NOT_FOUND.getCode());
                });
    }

    // ----------------------------------------------------------------
    // generateUploadUrl
    // ----------------------------------------------------------------

    @Test
    @DisplayName("generateUploadUrl: S3 키가 생성되고 FileMetadata가 저장된 뒤 Presigned URL이 반환된다")
    void generateUploadUrl_success() {
        // given
        QuestionSet questionSet = createQuestionSet(1L, AnalysisStatus.PENDING_UPLOAD);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(s3Service.getBucket()).willReturn("test-bucket");
        given(s3Service.generatePutPresignedUrl(anyString(), anyString()))
                .willReturn("https://s3.example.com/presigned-put");

        // 서비스 내부에서 새로 생성한 FileMetadata 인스턴스에 id를 주입해 반환한다.
        given(fileMetadataRepository.save(any(FileMetadata.class))).willAnswer(inv -> {
            FileMetadata saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        UploadUrlRequest request = new UploadUrlRequest();
        ReflectionTestUtils.setField(request, "contentType", "video/webm");

        // when
        UploadUrlResponse response = questionSetService.generateUploadUrl(5L, 1L, request);

        // then
        assertThat(response.getUploadUrl()).isEqualTo("https://s3.example.com/presigned-put");
        assertThat(response.getS3Key()).isEqualTo("videos/5/qs_1.webm");
        assertThat(response.getFileMetadataId()).isEqualTo(100L);
        then(fileMetadataRepository).should().save(any(FileMetadata.class));
    }

    // ----------------------------------------------------------------
    // getStatus
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getStatus: 질문세트 분석 상태가 정상 반환된다")
    void getStatus_success() {
        // given
        QuestionSet questionSet = createQuestionSet(1L, AnalysisStatus.ANALYZING);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));

        // when
        QuestionSetStatusResponse response = questionSetService.getStatus(1L);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZING);
    }

    @Test
    @DisplayName("getStatus: 존재하지 않는 질문세트 ID로 요청하면 BusinessException이 발생한다")
    void getStatus_notFound() {
        // given
        given(questionSetRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> questionSetService.getStatus(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo(QuestionSetErrorCode.NOT_FOUND.getCode());
                });
    }

    // ----------------------------------------------------------------
    // getFeedback
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getFeedback: 피드백과 스트리밍 URL이 포함된 응답이 반환된다")
    void getFeedback_success() {
        // given
        FileMetadata fileMetadata = FileMetadata.builder()
                .fileType(FileType.VIDEO)
                .s3Key("videos/5/qs_1.webm")
                .bucket("test-bucket")
                .contentType("video/webm")
                .build();
        ReflectionTestUtils.setField(fileMetadata, "streamingS3Key", "videos/5/qs_1.mp4");

        QuestionSet questionSet = createQuestionSet(1L, AnalysisStatus.COMPLETED);
        ReflectionTestUtils.setField(questionSet, "fileMetadata", fileMetadata);

        QuestionSetFeedback feedback = QuestionSetFeedback.builder()
                .questionSet(questionSet)
                .questionSetScore(85)
                .questionSetComment("전반적으로 좋은 답변입니다.")
                .build();
        ReflectionTestUtils.setField(feedback, "id", 50L);

        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(feedbackRepository.findByQuestionSetIdWithTimestampFeedbacks(1L))
                .willReturn(Optional.of(feedback));
        given(s3Service.generateGetPresignedUrl("videos/5/qs_1.mp4"))
                .willReturn("https://s3.example.com/streaming");
        given(s3Service.generateGetPresignedUrl("videos/5/qs_1.webm"))
                .willReturn("https://s3.example.com/fallback");

        // when
        QuestionSetFeedbackResponse response = questionSetService.getFeedback(1L);

        // then
        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getQuestionSetScore()).isEqualTo(85);
        assertThat(response.getStreamingUrl()).isEqualTo("https://s3.example.com/streaming");
        assertThat(response.getFallbackUrl()).isEqualTo("https://s3.example.com/fallback");
    }

    @Test
    @DisplayName("getFeedback: 피드백이 존재하지 않으면 BusinessException이 발생한다")
    void getFeedback_feedbackNotFound() {
        // given
        QuestionSet questionSet = createQuestionSet(1L, AnalysisStatus.COMPLETED);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(feedbackRepository.findByQuestionSetIdWithTimestampFeedbacks(1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> questionSetService.getFeedback(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo(QuestionSetErrorCode.FEEDBACK_NOT_FOUND.getCode());
                });
    }

    // ----------------------------------------------------------------
    // getQuestionsWithAnswers
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getQuestionsWithAnswers: 질문과 답변 타임스탬프가 매핑된 응답이 반환된다")
    void getQuestionsWithAnswers_success() {
        // given
        Question question = createQuestion(10L);
        QuestionSetAnswer answer = QuestionSetAnswer.builder()
                .question(question)
                .startMs(0L)
                .endMs(5000L)
                .build();

        given(questionRepository.findByQuestionSetIdOrderByOrderIndex(1L))
                .willReturn(List.of(question));
        given(answerRepository.findByQuestionSetIdWithQuestion(1L))
                .willReturn(List.of(answer));

        // when
        QuestionsWithAnswersResponse response = questionSetService.getQuestionsWithAnswers(1L);

        // then
        assertThat(response.getQuestions()).hasSize(1);
        assertThat(response.getQuestions().get(0).getQuestionId()).isEqualTo(10L);
        assertThat(response.getQuestions().get(0).getStartMs()).isEqualTo(0L);
        assertThat(response.getQuestions().get(0).getEndMs()).isEqualTo(5000L);
    }

    // ----------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------

    private QuestionSet createQuestionSet(Long id, AnalysisStatus status) {
        QuestionSet questionSet = QuestionSet.builder()
                .category(QuestionCategory.CS)
                .orderIndex(1)
                .build();
        ReflectionTestUtils.setField(questionSet, "id", id);
        // PENDING → target 으로 강제 전환
        if (status != AnalysisStatus.PENDING) {
            ReflectionTestUtils.setField(questionSet, "analysisStatus", status);
        }
        return questionSet;
    }

    private Question createQuestion(Long id) {
        Question question = Question.builder()
                .questionType(QuestionType.MAIN)
                .questionText("Java의 GC 동작 원리를 설명하세요.")
                .modelAnswer("Generational GC 기반으로 동작합니다.")
                .orderIndex(1)
                .build();
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }
}
