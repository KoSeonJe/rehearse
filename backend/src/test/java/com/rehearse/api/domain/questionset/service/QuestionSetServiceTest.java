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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;

@ExtendWith(MockitoExtension.class)
class QuestionSetServiceTest {

    @InjectMocks
    private QuestionSetService questionSetService;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionAnswerRepository answerRepository;

    @Mock
    private QuestionSetFeedbackRepository feedbackRepository;

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private QuestionSetAnalysisRepository analysisRepository;

    @Mock
    private S3Service s3Service;

    // ----------------------------------------------------------------
    // saveAnswers
    // ----------------------------------------------------------------

    @Test
    @DisplayName("saveAnswers: 답변 구간이 저장되고 질문세트 상태가 PENDING_UPLOAD로 변경된다")
    void saveAnswers_success() {
        // given
        QuestionSet questionSet = createQuestionSet(1L);
        // saveAnswers 내부에서 findOrCreateAnalysis 호출 시 새 analysis 반환
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.empty());
        given(analysisRepository.save(any(QuestionSetAnalysis.class))).willAnswer(inv -> inv.getArgument(0));

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
        // 멱등성 보장 — saveAll 이전에 deleteByQuestionSetId 가 먼저 호출되어야 한다
        var inOrder = inOrder(answerRepository);
        then(answerRepository).should(inOrder).deleteByQuestionSetId(1L);
        then(answerRepository).should(inOrder).flush();
        then(answerRepository).should(inOrder).saveAll(anyList());
        then(analysisRepository).should().save(any(QuestionSetAnalysis.class));
    }

    @Test
    @DisplayName("saveAnswers: 동일 questionSetId 로 두 번 호출해도 멱등적으로 동작한다 (중복 행 누적 방지)")
    void saveAnswers_idempotent() {
        // given
        QuestionSet questionSet = createQuestionSet(1L);
        given(questionSetRepository.findById(1L)).willReturn(Optional.of(questionSet));
        given(analysisRepository.findByQuestionSetId(1L)).willReturn(Optional.empty());
        given(analysisRepository.save(any(QuestionSetAnalysis.class))).willAnswer(inv -> inv.getArgument(0));

        Question question = createQuestion(10L);
        given(questionRepository.findById(10L)).willReturn(Optional.of(question));
        given(answerRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

        SaveAnswersRequest request = new SaveAnswersRequest();
        SaveAnswersRequest.AnswerTimestamp timestamp = new SaveAnswersRequest.AnswerTimestamp();
        ReflectionTestUtils.setField(timestamp, "questionId", 10L);
        ReflectionTestUtils.setField(timestamp, "startMs", 0L);
        ReflectionTestUtils.setField(timestamp, "endMs", 5000L);
        ReflectionTestUtils.setField(request, "answers", List.of(timestamp));

        // when — 같은 questionSet 에 대해 2 번 호출 (프론트 복구 루프 경합 시나리오)
        questionSetService.saveAnswers(1L, request);
        questionSetService.saveAnswers(1L, request);

        // then — 매 호출마다 delete 가 선행되므로 기존 행이 남지 않고 마지막 호출의 상태만 DB 에 반영된다
        then(answerRepository).should(times(2)).deleteByQuestionSetId(1L);
        then(answerRepository).should(times(2)).saveAll(anyList());
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
        QuestionSet questionSet = createQuestionSetWithAnalysis(1L, AnalysisStatus.PENDING_UPLOAD);
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
        QuestionSet questionSet = createQuestionSetWithAnalysis(1L, AnalysisStatus.ANALYZING);
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

        QuestionSet questionSet = createQuestionSetWithAnalysis(1L, AnalysisStatus.COMPLETED);
        ReflectionTestUtils.setField(questionSet, "fileMetadata", fileMetadata);

        QuestionSetFeedback feedback = QuestionSetFeedback.builder()
                .questionSet(questionSet)
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
        assertThat(response.getQuestionSetComment()).isEqualTo("전반적으로 좋은 답변입니다.");
        assertThat(response.getStreamingUrl()).isEqualTo("https://s3.example.com/streaming");
        assertThat(response.getFallbackUrl()).isEqualTo("https://s3.example.com/fallback");
    }

    @Test
    @DisplayName("getFeedback: 피드백이 존재하지 않으면 BusinessException이 발생한다")
    void getFeedback_feedbackNotFound() {
        // given
        QuestionSet questionSet = createQuestionSetWithAnalysis(1L, AnalysisStatus.COMPLETED);
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
        QuestionAnswer answer = QuestionAnswer.builder()
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

    @Test
    @DisplayName("getQuestionsWithAnswers: 답변 없는 질문(MAIN/FOLLOWUP 공통)은 결과에서 제외된다")
    void getQuestionsWithAnswers_filtersUnanswered() {
        // given
        Question answeredMain = createQuestion(10L);
        Question unansweredMain = createQuestion(11L);
        Question answeredFollowup = createFollowupQuestion(20L);
        Question unansweredFollowup = createFollowupQuestion(30L);

        QuestionAnswer mainAnswer = QuestionAnswer.builder()
                .question(answeredMain)
                .startMs(0L)
                .endMs(5000L)
                .build();
        QuestionAnswer followupAnswer = QuestionAnswer.builder()
                .question(answeredFollowup)
                .startMs(5000L)
                .endMs(10000L)
                .build();

        given(questionRepository.findByQuestionSetIdOrderByOrderIndex(1L))
                .willReturn(List.of(answeredMain, unansweredMain, answeredFollowup, unansweredFollowup));
        given(answerRepository.findByQuestionSetIdWithQuestion(1L))
                .willReturn(List.of(mainAnswer, followupAnswer));

        // when
        QuestionsWithAnswersResponse response = questionSetService.getQuestionsWithAnswers(1L);

        // then — 답변 있는 질문만 포함 (unansweredMain=11L, unansweredFollowup=30L 제외)
        assertThat(response.getQuestions()).hasSize(2);
        assertThat(response.getQuestions())
                .extracting(QuestionsWithAnswersResponse.QuestionWithAnswer::getQuestionId)
                .containsExactly(10L, 20L);
    }

    // ----------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------

    private QuestionSet createQuestionSet(Long id) {
        QuestionSet questionSet = QuestionSet.builder()
                .category(QuestionSetCategory.CS_FUNDAMENTAL)
                .orderIndex(1)
                .build();
        ReflectionTestUtils.setField(questionSet, "id", id);
        return questionSet;
    }

    private QuestionSet createQuestionSetWithAnalysis(Long id, AnalysisStatus status) {
        QuestionSet questionSet = createQuestionSet(id);
        QuestionSetAnalysis analysis = QuestionSetAnalysis.builder()
                .questionSet(questionSet)
                .build();
        if (status != AnalysisStatus.PENDING) {
            ReflectionTestUtils.setField(analysis, "analysisStatus", status);
        }
        ReflectionTestUtils.setField(questionSet, "analysis", analysis);
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

    private Question createFollowupQuestion(Long id) {
        Question question = Question.builder()
                .questionType(QuestionType.FOLLOWUP)
                .questionText("GC 튜닝 경험이 있으신가요?")
                .modelAnswer("G1GC 튜닝 사례를 설명합니다.")
                .orderIndex(2)
                .build();
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }
}
